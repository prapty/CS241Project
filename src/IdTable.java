import java.util.HashMap;
import java.util.Map;

public class IdTable {
    int availableId;
    Map<String, Integer> identityMap;

    public IdTable() {
        availableId=1;
        identityMap=new HashMap<>();
        String reservedWordCommonPart="DefaultId";
        int reservedWordCommonLength=reservedWordCommonPart.length();
        for (ReservedWords identity : ReservedWords.values()) {
            String wordName=identity.name();
            int wordLength=wordName.length();
            int keyLength=wordLength-reservedWordCommonLength;
            String key=wordName.substring(0,keyLength);
            identityMap.put(key, identity.ordinal());
            availableId++;
        }
    }
    public int getID(String identity){
        Integer id=identityMap.get(identity);
        if(id==null){
            id=availableId;
            identityMap.put(identity, id);
            availableId++;
        }
        return id;
    }
}
