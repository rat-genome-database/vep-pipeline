package edu.mcw.rgd.vep;

import edu.mcw.rgd.datamodel.variants.VariantMapData;
import edu.mcw.rgd.process.Utils;
import htsjdk.samtools.util.BlockCompressedOutputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.FileSystemResource;

import java.io.BufferedWriter;
import java.io.File;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author mtutaj
 * @since 3/01/19
 */
public class Main {

    private DAO dao = new DAO();
    private String version;
    private Map<String,String> seqRetrieveUrl;

    Logger log = LogManager.getLogger("status");

    public static void main(String[] args) throws Exception {

        DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
        new XmlBeanDefinitionReader(bf).loadBeanDefinitions(new FileSystemResource("properties/AppConfigure.xml"));
        Main manager = (Main) (bf.getBean("manager"));

        int mapKey = 0;
        String chrFilter = null;
        String outDir = "/tmp";
        String env = "dev";
        for( int i=0; i<args.length; i++ ) {
            if( "--mapKey".equals(args[i]) && i+1<args.length ) {
                mapKey = Integer.parseInt(args[++i]);
            } else if( "--chr".equals(args[i]) && i+1<args.length ) {
                chrFilter = args[++i];
            } else if( "--outDir".equals(args[i]) && i+1<args.length ) {
                outDir = args[++i];
            } else if( "--env".equals(args[i]) && i+1<args.length ) {
                env = args[++i];
            }
        }
        if( mapKey==0 ) {
            System.err.println("Usage: --mapKey N [--chr CHR] [--outDir DIR] [--env prod|dev]");
            System.exit(1);
        }

        try {
            manager.run(mapKey, chrFilter, outDir, env);
        }catch (Exception e) {
            Utils.printStackTrace(e, manager.log);
            throw e;
        }
    }

    public void run(int mapKey, String chrFilter, String outDir, String env) throws Exception {

        long startTime = System.currentTimeMillis();

        String msg = getVersion();
        log.info(msg);

        msg = dao.getConnectionInfo();
        log.info("   "+msg);

        SimpleDateFormat sdt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        log.info("   started at "+sdt.format(new Date(startTime)));

        String assemblyName = dao.getMapName(mapKey);
        log.info("   assembly: "+assemblyName+" (mapKey="+mapKey+")");

        String seqRetrieveUrl = this.seqRetrieveUrl.getOrDefault(env, this.seqRetrieveUrl.get("dev"));
        log.info("   environment: "+env+"  (seqretrieve: "+seqRetrieveUrl+")");

        List<String> chromosomes;
        if( chrFilter!=null ) {
            chromosomes = Collections.singletonList(chrFilter);
            log.info("   chromosome filter: "+chrFilter);
        } else {
            chromosomes = dao.getChromosomeNames(mapKey);
        }

        String outFile = new File(outDir, assemblyName+".vcf.gz").getAbsolutePath();
        log.info("   writing to "+outFile);

        // BGZF (bgzip) format -- a valid gzip file that also supports tabix indexing
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
                new BlockCompressedOutputStream(new File(outFile)), StandardCharsets.UTF_8));
        out.write("##fileformat=VCFv4.0\n");
        out.write("#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\n");

        for( String chr: chromosomes ) {
            List<VariantMapData> variants = dao.getVariants(mapKey, chr);
            log.info("  chr "+chr+", variants read = " + Utils.formatThousands(variants.size()));

            // an indel (empty REF or empty ALT) needs a single-base upstream anchor to be valid VCF;
            // for variants with no padding base stored, fetch that base from the reference sequence
            int minNeeded = Integer.MAX_VALUE, maxNeeded = 0;
            for( VariantMapData d: variants ) {
                if( needsFetchedPaddingBase(d) && d.getStartPos()>1 ) {
                    int p = (int)(d.getStartPos()-1);
                    minNeeded = Math.min(minNeeded, p);
                    maxNeeded = Math.max(maxNeeded, p);
                }
            }
            String refSeq = null;
            int refSeqStart = minNeeded;
            if( maxNeeded>0 ) {
                refSeq = dao.getReferenceSequence(seqRetrieveUrl, mapKey, chr, minNeeded, maxNeeded);
                log.info("    fetched reference sequence "+chr+":"+minNeeded+"-"+maxNeeded+" to supply missing padding bases");
            }

            List<String> dataLines = new ArrayList<>(variants.size());
            int skipped = 0;
            for (VariantMapData d : variants) {

                long pos = d.getStartPos();
                String refNuc = Utils.defaultString(d.getReferenceNucleotide());
                String varNuc = Utils.defaultString(d.getVariantNucleotide());

                String paddingBase = Utils.defaultString(d.getPaddingBase());
                if( paddingBase.isEmpty() && needsFetchedPaddingBase(d) && refSeq!=null ) {
                    int idx = (int)(d.getStartPos()-1) - refSeqStart;
                    if( idx>=0 && idx<refSeq.length() ) {
                        paddingBase = refSeq.substring(idx, idx+1).toUpperCase();
                    }
                }
                if( !paddingBase.isEmpty() ) {
                    pos -= paddingBase.length();
                    refNuc = paddingBase + refNuc;
                    varNuc = paddingBase + varNuc;
                }

                // never emit an empty REF or ALT -- that is malformed VCF
                if( refNuc.isEmpty() || varNuc.isEmpty() ) {
                    log.warn("    skipped variant lacking a padding base: "+chr+":"+d.getStartPos()+" id="+d.getId());
                    skipped++;
                    continue;
                }

                dataLines.add(d.getChromosome()+"\t"+pos+"\t"+d.getId()+"\t"+refNuc+"\t"+varNuc+"\t.\t.\t.\n");
            }
            if( skipped>0 ) {
                log.warn("  chr "+chr+": skipped "+skipped+" variant(s) with no padding base available");
            }

            // VCF requires records ordered by position within a chromosome;
            // the padding base shifts POS, so sort on the final POS
            dataLines.sort( (l1, l2) -> {
                long p1 = posOf(l1), p2 = posOf(l2);
                return p1!=p2 ? Long.compare(p1, p2) : l1.compareTo(l2);
            });

            for( String line: dataLines ) {
                out.write(line);
            }
        }

        out.close();

        log.info("");
        log.info("===    time elapsed: "+ Utils.formatElapsedTime(startTime, System.currentTimeMillis()));
        log.info("");
    }


    // a pure insertion (empty REF) or pure deletion (empty ALT) with no padding base stored
    private static boolean needsFetchedPaddingBase(VariantMapData d) {
        return Utils.isStringEmpty(d.getPaddingBase())
                && (Utils.isStringEmpty(d.getReferenceNucleotide()) || Utils.isStringEmpty(d.getVariantNucleotide()));
    }

    // extract the POS field (2nd column) from a rendered VCF data line
    private static long posOf(String vcfLine) {
        int t1 = vcfLine.indexOf('\t');
        int t2 = vcfLine.indexOf('\t', t1+1);
        return Long.parseLong(vcfLine.substring(t1+1, t2));
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }

    public void setSeqRetrieveUrl(Map<String,String> seqRetrieveUrl) {
        this.seqRetrieveUrl = seqRetrieveUrl;
    }

    public Map<String,String> getSeqRetrieveUrl() {
        return seqRetrieveUrl;
    }
}

