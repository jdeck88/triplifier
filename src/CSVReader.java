
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StreamTokenizer;
import java.util.LinkedList;
import java.util.NoSuchElementException;


/**
 * Provides the ability to parse CSV formatted files.  This implementation is
 * mostly compliant with the RFC 4180 CSV specification.  In particular, 1) each
 * record is expected to be on a single line; 2) fields may be enclosed in
 * double quotes; 3) double quotes inside of a double-quoted string should be
 * escaped with a double quote; 4) space and tab characters are not trimmed from
 * the beginning or end of fields.
 * 
 * This implementation has some additional features and/or limitations that
 * might not be supported by other CSV libraries.  First, if a field is
 * double-quoted, there should be no additional characters outside of the double
 * quotes.  Second, records must not span multiple lines.  Third, standard
 * escape sequences (e.g., "\"\t") are also supported inside of quoted fields.
 * Fourth, blank lines are ignored.
 * 
 * Finally, note that ASCII characters 0-8 and 10-31 are all treated as empty
 * whitespace and ignored (unless they occur within a quoted string).
 */
public class CSVReader implements TabularDataReader
{
    private StreamTokenizer st;
    private boolean hasnext = false;
    private LinkedList<String> recqueue;
    
    public CSVReader()
    {
        recqueue = new LinkedList<String>();
    }
    
    @Override
    public String getSourceFormat() {
        return "CSV";
    }

    @Override
    public String getFormatDescription() {
        return "comma-separated value";
    }

    @Override
    public String getFileExtension() {
        return "csv";
    }

    /** See if the specified file is a CSV file.  Since no "magic number" can
     * be defined for CSV files, this test is limited to seeing if the file
     * extension is "csv".  This method only examines the specified file name
     * and does not currently test if the file actually exists.
     * 
     * @param filepath The file to test.
     * 
     * @return True if the specified file appears to be a CSV file, false
     * otherwise.
     */
    @Override
    public boolean testFile(String filepath) {
        int index = filepath.lastIndexOf('.');
        
        if (index != -1 && index != (filepath.length() - 1)) {
            // get the extension
            String ext = filepath.substring(index + 1);
            
            if (ext.equals("csv"))
                return true;            
        }
        
        return false;
    }
    
    @Override
    public boolean openFile(String filepath) {
        try {
            st = new StreamTokenizer(new FileReader(filepath));
        }
        catch (FileNotFoundException e) {
            return false;
        }
        
        st.resetSyntax();
        st.eolIsSignificant(true);
        st.whitespaceChars(0, 31);
        st.wordChars(' ', 255);
        st.wordChars('\t', '\t');
        st.quoteChar('"');
        st.ordinaryChar(',');
        
        testNext();
        
        return true;
    }

    /**
     * Internal method to see if there is another line with data remaining in
     * the file.  Any completely blank lines will be skipped.  At exit, st.ttype
     * will be the first token of the next record in the file.
     */
    private void testNext() {
        int tokentype = StreamTokenizer.TT_EOF;
        
        do {
            try { tokentype = st.nextToken(); }
            catch (IOException e) {}
        } while (
                tokentype == StreamTokenizer.TT_EOL
                );
        
        if (tokentype != StreamTokenizer.TT_EOF)
            hasnext = true;
        else
            hasnext = false;
    }
    
    @Override
    public boolean hasNextRow() {
        return hasnext;
    }

    @Override
    public String[] getNextRow() {
        if (!hasNextRow())
            throw new NoSuchElementException();
        
        int prevToken = ',';
        int fieldcnt = 0;
        recqueue.clear();
        
        try {
        while (st.ttype != StreamTokenizer.TT_EOL &&
                st.ttype != StreamTokenizer.TT_EOF) {
            if (st.ttype == ',') {
                // See if we just passed an empty field.
                if (prevToken == ',') {
                    recqueue.add("");
                    fieldcnt++;
                }
            }
            else {
                // See if we just passed an escaped double quote inside
                // of a quoted string.
                if (prevToken != ',')
                    recqueue.add(recqueue.removeLast() + "\"" + st.sval);
                else {
                    recqueue.add(st.sval);
                    fieldcnt++;
                }
            }
            
            prevToken = st.ttype;
            st.nextToken();
        }
        }
        catch (IOException e) {}
        
        // Test for the special case of a blank last field.
        if (prevToken == ',') {
            recqueue.add("");
            fieldcnt++;
        }
        
        testNext();
        
        //while (!recqueue.isEmpty())
        //    System.out.println(recqueue.remove());
        
        String[] ret = new String[fieldcnt];
        for (int cnt = 0; cnt < fieldcnt; cnt++)
            ret[cnt] = recqueue.remove();
        
        return ret;
    }

    @Override
    public void closeFile() {
    }
}
