package edu.mcw.rgd.vep;

import edu.mcw.rgd.dao.impl.MapDAO;
import edu.mcw.rgd.dao.impl.variants.VariantDAO;
import edu.mcw.rgd.datamodel.Chromosome;
import edu.mcw.rgd.datamodel.variants.VariantMapData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author mtutaj
 * @since 4/11/2023
 * <p>
 * wrapper to handle all DAO code
 */
public class DAO {

    MapDAO mapDAO = new MapDAO();
    VariantDAO vdao = new VariantDAO();

    public String getConnectionInfo() {
        return vdao.getConnectionInfo();
    }

    public String getMapName( int mapKey ) throws Exception {
        return mapDAO.getMap(mapKey).getName();
    }

    public List<String> getChromosomeNames( int mapKey ) throws Exception {

        List<String> chrNames = new ArrayList<>();
        List<Chromosome> list = mapDAO.getChromosomes(mapKey);
        for( Chromosome c: list ) {
            chrNames.add(c.getChromosome());
        }
        Collections.sort(chrNames);
        return chrNames;
    }

    public List<VariantMapData> getVariants(int mapKey, String chr) throws Exception {
        List<VariantMapData> variants = vdao.getVariantsWithGeneLocation(mapKey, chr, 1, Integer.MAX_VALUE);
        return variants;
    }
}
