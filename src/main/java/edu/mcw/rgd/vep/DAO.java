package edu.mcw.rgd.vep;

import edu.mcw.rgd.dao.impl.variants.VariantDAO;
import edu.mcw.rgd.datamodel.variants.VariantMapData;

import java.util.List;

/**
 * @author mtutaj
 * @since 4/11/2023
 * <p>
 * wrapper to handle all DAO code
 */
public class DAO {

    VariantDAO vdao = new VariantDAO();
    /*

    Logger logInserted = LogManager.getLogger("inserted");
    Logger logDeleted = LogManager.getLogger("deleted");
*/
    public String getConnectionInfo() {
        return vdao.getConnectionInfo();
    }

    public List<VariantMapData> getVariants(int mapKey, String chr) throws Exception {
        List<VariantMapData> variants = vdao.getVariantsWithGeneLocation(mapKey, chr, 1, Integer.MAX_VALUE);
        return variants;
    }

/*

    public List<Transcript> getTranscriptsByAccId(String accId) throws Exception {
        return tdao.getTranscriptsByAccId(accId);
    }

    public List<XdbId> getRNACentralIds(int speciesTypeKey, String srcPipeline, int xdbKey) throws Exception {

        XdbId filter = new XdbId();
        filter.setXdbKey(xdbKey);
        filter.setSrcPipeline(srcPipeline);
        return xdao.getXdbIds(filter, speciesTypeKey);
    }

    public List<Gene> getActiveGeneIdsForRefseqAcc(String acc) throws Exception {
        return xdao.getActiveGenesByXdbId(XdbId.XDB_KEY_GENEBANKNU, acc);
    }

    public int insertXdbs(Collection<XdbId> xdbs) throws Exception {

        for( XdbId xdbId: xdbs ) {
            logInserted.debug(xdbId.dump("|"));
        }

        return xdao.insertXdbs(new ArrayList<>(xdbs));
    }

    public int deleteXdbIds( Collection<XdbId> xdbIds ) throws Exception {

        for( XdbId xdbId: xdbIds ) {
            logDeleted.debug(xdbId.dump("|"));
        }

        return xdao.deleteXdbIds(new ArrayList<>(xdbIds));
    }

    public int updateModificationDate(Collection<XdbId> xdbIds) throws Exception {

        List<Integer> xdbKeys = new ArrayList<Integer>(xdbIds.size());
        for( XdbId xdbId: xdbIds ) {
            xdbKeys.add(xdbId.getKey());
        }
        return xdao.updateModificationDate(xdbKeys);
    }
    */
}
