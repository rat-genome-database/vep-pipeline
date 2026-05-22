package edu.mcw.rgd.vep;

import edu.mcw.rgd.dao.impl.MapDAO;
import edu.mcw.rgd.dao.impl.variants.VariantDAO;
import edu.mcw.rgd.datamodel.Chromosome;
import edu.mcw.rgd.datamodel.variants.VariantMapData;

import java.util.ArrayList;
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
        // numeric chromosomes first, ordered as numbers; then the rest, ordered alphabetically
        chrNames.sort((c1, c2) -> {
            boolean n1 = c1.matches("\\d+");
            boolean n2 = c2.matches("\\d+");
            if( n1 && n2 ) {
                return Integer.compare(Integer.parseInt(c1), Integer.parseInt(c2));
            }
            if( n1 != n2 ) {
                return n1 ? -1 : 1;
            }
            return c1.compareTo(c2);
        });
        return chrNames;
    }

    public List<VariantMapData> getVariants(int mapKey, String chr) throws Exception {
        List<VariantMapData> variants = vdao.getVariantsWithGeneLocation(mapKey, chr, 1, Integer.MAX_VALUE);
        return variants;
    }
}
