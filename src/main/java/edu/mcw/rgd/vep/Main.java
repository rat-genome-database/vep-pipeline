package edu.mcw.rgd.vep;

import edu.mcw.rgd.dao.impl.variants.VariantDAO;
import edu.mcw.rgd.datamodel.variants.VariantMapData;
import edu.mcw.rgd.process.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.FileSystemResource;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
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

        try {
            manager.run();
        }catch (Exception e) {
            Utils.printStackTrace(e, manager.log);
            throw e;
        }
    }

    public void run() throws Exception {

        long startTime = System.currentTimeMillis();

        String msg = getVersion();
        log.info(msg);

        msg = dao.getConnectionInfo();
        log.info("   "+msg);

        SimpleDateFormat sdt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        log.info("   started at "+sdt.format(new Date(startTime)));

        List<VariantMapData> variants = dao.getVariants(372, "20");
        log.info("variants read = "+variants.size());

        // sort variants by chromosome and position
        Collections.sort(variants, new Comparator<VariantMapData>() {
            @Override
            public int compare(VariantMapData d1, VariantMapData d2) {
                int r = d1.getChromosome().compareTo(d2.getChromosome());
                if( r!=0 ) {
                    return r;
                }
                return (int) (d1.getStartPos() - d2.getStartPos());
            }
        });
        log.info("variants sorted");

        BufferedWriter out = Utils.openWriter("/tmp/rn7_chr20.vcf");
        out.write("##fileformat=VCFv4.0\n");
        out.write("#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\n");

        for( VariantMapData d: variants ) {

            long pos = d.getStartPos();
            String refNuc = Utils.defaultString(d.getReferenceNucleotide());
            String varNuc = Utils.defaultString(d.getVariantNucleotide());

            int paddingBaseLen = Utils.defaultString(d.getPaddingBase()).length();
            if( paddingBaseLen > 0 ) {
                pos -= paddingBaseLen;
                refNuc = d.getPaddingBase() + refNuc;
                varNuc = d.getPaddingBase() + varNuc;
            }

            out.write(d.getChromosome()+"\t"
                    +pos+"\t"
                    +d.getId()+"\t"
                    +refNuc+"\t"
                    +varNuc+"\t"
                    +".\t.\t.\n");
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

