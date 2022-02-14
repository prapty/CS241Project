import java.io.IOException;

public class Lexer {
    Reader reader;
    char sym;
    IdTable idTable;
    ReservedSymbols reservedSymbols;
    int reservedWordSeparator;

    public Lexer(String fileName) throws IOException {
        reader = new Reader(fileName);
        sym = reader.next();
        idTable = new IdTable();
        reservedSymbols = new ReservedSymbols();
        reservedWordSeparator = ReservedWords.values().length;
    }

    Token nextToken() throws IOException, SyntaxException {
        Token tok = new Token();
        if (sym == ' ' || sym == '\n' || sym == '\r' || sym == '\t') {
            sym = reader.next();
            tok = nextToken();
        } else if ((sym >= 'a' && sym <= 'z')||(sym >= 'A' && sym <= 'Z')) {
            tok = getWord();
        } else if (sym >= '0' && sym <= '9') {
            tok = getNumber();
        } else if (sym == '=' || sym == '<' || sym == '>' || sym == '!') {
            //get relOp or <-
            tok = getRelOp();
        } else {
            tok = getSymbol();
            //get ( [ , ; + - * / { .   single char, or error
        }
        return tok;
    }

    private Token getWord() throws IOException { //check if reserved word or identifier, return token
        String word = "" + sym;
        sym = reader.next();
        while ((sym >= '0' && sym <= '9') || (sym >= 'a' && sym <= 'z') || (sym >= 'A' && sym <= 'Z')) {
            word += sym;
            sym = reader.next();
        }
        TokenKind kind = TokenKind.identity;
        int id = idTable.getID(word);
        if (id <= reservedWordSeparator) {
            kind = TokenKind.reservedWord;
        }
        Token token = new Token(id, kind, true);
        return token;
    }

    private Token getNumber() throws IOException {
        String num = "" + sym;
        sym = reader.next();
        while (sym >= '0' && sym <= '9') {
            num += sym;
            sym = reader.next();
        }
        int number = Integer.parseInt(num);
        TokenKind kind = TokenKind.number;
        Token token = new Token(number, kind, false);
        return token;
    }

    private Token getRelOp() throws IOException {
        String op = "" + sym;
        sym = reader.next();
        String extendedOp = op + sym;
        String wordSymbol = reservedSymbols.wordSymbolMap.get(extendedOp);
        if (wordSymbol == null) {
            wordSymbol = reservedSymbols.wordSymbolMap.get(op);
        } else {
            sym = reader.next();
        }
        String reservedWordCommonPart = "DefaultId";
        int reservedWordCommonLength = reservedWordCommonPart.length();
        int wordLength = wordSymbol.length();
        int keyLength = wordLength - reservedWordCommonLength;
        String key = wordSymbol.substring(0, keyLength);
        int id = idTable.getID(key);
        TokenKind kind = TokenKind.relOp;
        Token token = new Token(id, kind, true);
        return token;
    }

    private Token getSymbol() throws IOException, SyntaxException {
        String op = "" + sym;
        sym = reader.next();
        String wordSymbol = reservedSymbols.wordSymbolMap.get(op);
        if (wordSymbol == null) {
            error(ErrorInfo.INVALID_SYMBOL_LEXER_ERROR, op);
        }
        String reservedWordCommonPart = "DefaultId";
        int reservedWordCommonLength = reservedWordCommonPart.length();
        int wordLength = wordSymbol.length();
        int keyLength = wordLength - reservedWordCommonLength;
        String key = wordSymbol.substring(0, keyLength);
        int id = idTable.getID(key);
        TokenKind kind = TokenKind.reservedSymbol;
        Token token = new Token(id, kind, true);
        return token;
    }

    private void error(String message, String op) throws SyntaxException {
        String errorMessage = String.format(message, op);
        throw new SyntaxException(errorMessage);
    }
}
