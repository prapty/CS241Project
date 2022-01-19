import java.io.FileNotFoundException;
import java.io.IOException;

public class Lexer {
    public static void main(String[] args) {
        System.out.println("Hello world");
    }

    Reader reader;
    char sym;

    public Lexer(String fileName) throws IOException {
        reader = new Reader(fileName);
        sym = reader.next();
    }

    void nextToken() throws IOException {
        String tok = "";
        if (sym == ' ' || sym == '\n') {
            nextToken();
        } else if (sym >= 'a' && sym <= 'z') {
            getWord(sym, reader);
            //make token
        } else if (sym >= '0' && sym <= '9') {
            getNumber(sym, reader);
            //make token
        } else if (sym == '=' || sym == '<' || sym == '>' || sym == '!') {
            //get relOp or <-
            getRelOp(sym, reader);
            //make token
        } else {
            //get ( [ , ; + - * / { .   single char, or error
            // return token or error
        }
        //return token
    }

    private void getWord(char sym, Reader reader) throws IOException {
        String word = "" + sym;
        sym = reader.next();
        while ((sym >= '0' && sym <= '9') || (sym >= 'a' && sym <= 'z')) {
            word += sym;
            sym = reader.next();
        }
        //check if reserved word or identifier, return token
    }

    private void getNumber(char sym, Reader reader) throws IOException {
        String num = "" + sym;
        sym = reader.next();
        while (sym >= '0' && sym <= '9') {
            num += sym;
            sym = reader.next();
        }
        int number = Integer.parseInt(num);
        //return token
    }

    private void getRelOp(char sym, Reader reader) throws IOException {
        String op = "" + sym + reader.next();
        sym = reader.next();
        //return token
    }

}
