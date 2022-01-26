import java.util.HashMap;
import java.util.Map;

public class ReservedSymbols {
    Map<String, String> wordSymbolMap;

    public ReservedSymbols() {
        wordSymbolMap = new HashMap<>();
        wordSymbolMap.put("==", "equalToDefaultId");
        wordSymbolMap.put("!=", "notEqualToDefaultId");
        wordSymbolMap.put("<", "lessThanDefaultId");
        wordSymbolMap.put("<=", "lessThanOrEqualToDefaultId");
        wordSymbolMap.put(">", "greaterThanDefaultId");
        wordSymbolMap.put(">=", "greaterThanOrEqualToDefaultId");
        wordSymbolMap.put("(", "startingFirstBracketDefaultId");
        wordSymbolMap.put(")", "endingFirstBracketDefaultId");
        wordSymbolMap.put("[", "startingThirdBracketDefaultId");
        wordSymbolMap.put("]", "endingThirdBracketDefaultId");
        wordSymbolMap.put("{", "startingCurlyBracketDefaultId");
        wordSymbolMap.put("}", "endingCurlyBracketDefaultId");
        wordSymbolMap.put("+", "plusDefaultId");
        wordSymbolMap.put("-", "minusDefaultId");
        wordSymbolMap.put("*", "mulDefaultId");
        wordSymbolMap.put("/", "divDefaultId");
        wordSymbolMap.put("<-", "assignmentSymbolDefaultId");
        wordSymbolMap.put(",", "commaDefaultId");
        wordSymbolMap.put(";", "semicolonDefaultId");
        wordSymbolMap.put(".", "dotDefaultId");
    }
}
