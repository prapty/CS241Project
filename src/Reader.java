import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class Reader {

    FileReader fileReader;
    BufferedReader br;
    char sym;

    public Reader(String fileName) throws FileNotFoundException {
        fileReader = new FileReader(fileName);
        br = new BufferedReader(fileReader);
    }

    public char next() throws IOException {
        sym = (char) br.read();
        return sym;
    }
}
