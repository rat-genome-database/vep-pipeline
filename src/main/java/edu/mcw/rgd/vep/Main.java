package edu.mcw.rgd.vep;

import edu.mcw.rgd.datamodel.variants.VariantMapData;
import edu.mcw.rgd.process.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.FileSystemResource;

import java.io.BufferedWriter;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author mtutaj
 * @since 3/01/19
 */
public class Main {

    private DAO dao = new DAO();
    private String version;

    Logger log = LogManager.getLogger("status");

    public static void main(String[] args) throws Exception {

        DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
        new XmlBeanDefinitionReader(bf).loadBeanDefinitions(new FileSystemResource("properties/AppConfigure.xml"));
        Main manager = (Main) (bf.getBean("manager"));

        int mapKey = 0;
        String chrFilter = null;
        for( int i=0; i<args.length; i++ ) {
            if( "--mapKey".equals(args[i]) && i+1<args.length ) {
                mapKey = Integer.parseInt(args[++i]);
            } else if( "--chr".equals(args[i]) && i+1<args.length ) {
                chrFilter = args[++i];
            }
        }
        if( mapKey==0 ) {
            System.err.println("Usage: --mapKey N [--chr CHR]");
            System.exit(1);
        }

        try {
            manager.run(mapKey, chrFilter);
        }catch (Exception e) {
            Utils.printStackTrace(e, manager.log);
            throw e;
        }
    }

    public void run(int mapKey, String chrFilter) throws Exception {

        long startTime = System.currentTimeMillis();

        String msg = getVersion();
        log.info(msg);

        msg = dao.getConnectionInfo();
        log.info("   "+msg);

        SimpleDateFormat sdt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        log.info("   started at "+sdt.format(new Date(startTime)));

        String assemblyName = dao.getMapName(mapKey);
        log.info("   assembly: "+assemblyName+" (mapKey="+mapKey+")");

        List<String> chromosomes;
        if( chrFilter!=null ) {
            chromosomes = Collections.singletonList(chrFilter);
            log.info("   chromosome filter: "+chrFilter);
        } else {
            chromosomes = dao.getChromosomeNames(mapKey);
        }

        BufferedWriter out = Utils.openWriter("/tmp/"+assemblyName+".vcf.gz");
        out.write("##fileformat=VCFv4.0\n");
        out.write("#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\n");

        for( String chr: chromosomes ) {
            List<VariantMapData> variants = dao.getVariants(mapKey, chr);
            log.info("  chr "+chr+", variants read = " + Utils.formatThousands(variants.size()));

            // sort variants by chromosome and position
            Collections.sort(variants, new Comparator<VariantMapData>() {
                @Override
                public int compare(VariantMapData d1, VariantMapData d2) {
                    int r = d1.getChromosome().compareTo(d2.getChromosome());
                    if (r != 0) {
                        return r;
                    }
                    return (int) (d1.getStartPos() - d2.getStartPos());
                }
            });
            log.debug("    variants sorted");

            for (VariantMapData d : variants) {

                long pos = d.getStartPos();
                String refNuc = Utils.defaultString(d.getReferenceNucleotide());
                String varNuc = Utils.defaultString(d.getVariantNucleotide());

                int paddingBaseLen = Utils.defaultString(d.getPaddingBase()).length();
                if (paddingBaseLen > 0) {
                    pos -= paddingBaseLen;
                    refNuc = d.getPaddingBase() + refNuc;
                    varNuc = d.getPaddingBase() + varNuc;
                }

                out.write(d.getChromosome() + "\t"
                        + pos + "\t"
                        + d.getId() + "\t"
                        + refNuc + "\t"
                        + varNuc + "\t"
                        + ".\t.\t.\n");
            }

        }

        out.close();

        log.info("");
        log.info("===    time elapsed: "+ Utils.formatElapsedTime(startTime, System.currentTimeMillis()));
        log.info("");
    }


    public void setVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }
}

