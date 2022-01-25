import java.io.IOException;

public class Lexer {
    public static void main(String[] args) {
        System.out.println("Hello world");
    }

    Reader reader;
    char sym;
    IdTable idTable;
    ReservedSymbols reservedSymbols;
    int reservedWordSeparator;

    public Lexer(String fileName) throws IOException {
        reader = new Reader(fileName);
        sym = reader.next();
        idTable=new IdTable();
        reservedSymbols=new ReservedSymbols();
        reservedWordSeparator=ReservedWords.values().length;
    }

    Token nextToken() throws IOException, SyntaxException {
        Token tok=new Token();
        if (sym == ' ' || sym == '\n') {
            sym=reader.next();
            nextToken();
        } else if (sym >= 'a' && sym <= 'z') {
            tok=getWord(reader);
            //make token
        } else if (sym >= '0' && sym <= '9') {
            tok=getNumber(reader);
            //make token
        } else if (sym == '=' || sym == '<' || sym == '>' || sym == '!') {
            //get relOp or <-
            tok=getRelOp(reader);
            //make token
        } else {
            tok=getSymbol(reader);
            //get ( [ , ; + - * / { .   single char, or error
            // return token or error
        }
        return tok;
        //return token
    }

    private Token getWord(Reader reader) throws IOException {
        String word = "" + sym;
        sym = reader.next();
        while ((sym >= '0' && sym <= '9') || (sym >= 'a' && sym <= 'z')) {
            word += sym;
            sym = reader.next();
        }
        TokenKind kind=TokenKind.identity;
        int id=idTable.getID(word);
        if(id<=reservedWordSeparator){
            kind=TokenKind.reservedWord;
        }
        Token token = new Token(id, kind, true);
        return token;
        //check if reserved word or identifier, return token
    }

    private Token getNumber(Reader reader) throws IOException {
        String num = "" + sym;
        sym = reader.next();
        while (sym >= '0' && sym <= '9') {
            num += sym;
            sym = reader.next();
        }
        int number = Integer.parseInt(num);
        TokenKind kind= TokenKind.number;
        Token token = new Token(number, kind, false);
        return token;
        //return token
    }

    private Token getRelOp(Reader reader) throws IOException {
        String op = "" + sym;
        sym = reader.next();
        String extendedOp=op+sym;
        String wordSymbol=reservedSymbols.wordSymbolMap.get(extendedOp);
        if(wordSymbol==null){
            wordSymbol=reservedSymbols.wordSymbolMap.get(op);
        }
        else{
            sym=reader.next();
        }
        int id=idTable.getID(wordSymbol);
        TokenKind kind=TokenKind.relOp;
        Token token = new Token(id, kind, true);
        return token;
        //return token
    }

    private Token getSymbol(Reader reader) throws IOException, SyntaxException {
        String op = "" + sym;
        sym = reader.next();
        String wordSymbol=reservedSymbols.wordSymbolMap.get(op);
        if(wordSymbol==null){
            String errorMessage=String.format(ErrorInfo.INVALID_SYMBOL_LEXER_ERROR,op);
            throw new SyntaxException(errorMessage);
        }
        int id=idTable.getID(wordSymbol);
        TokenKind kind=TokenKind.reservedSymbol;
        Token token = new Token(id, kind, true);
        return token;
        //return token
    }
}
