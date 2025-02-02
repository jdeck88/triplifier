package reader;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.net.URL;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;

import reader.plugins.TabularDataReader;


/**
 * Provides high-level access to the Triplifier's data reader plugin system.
 * Includes methods for opening data files and getting instances of particular
 * reader plugins.  There should generally be few situations where you might
 * need to manually instantiate plugin classes.  Using the methods in
 * ReaderManager is much simpler, less error-prone, and recommended whenever
 * possible.
 */
public class ReaderManager implements Iterable<TabularDataReader> {
    private LinkedList<TabularDataReader> readers;

    /**
     * Initializes a new ReaderManager.  No plugins are loaded by default.  The
     * LoadReaders() method must be called to find and load reader plugins.
     */
    public ReaderManager() {
        readers = new LinkedList<TabularDataReader>();
    }

    /**
     * Load all reader plugins.  All compiled class files in the
     * reader/plugins directory will be examined to see if they implement the
     * TabularDataReader interface.  If so, they will be loaded as valid reader
     * plugins for use by the ReaderManager.
     *
     * @throws FileNotFoundException
     */
    public void loadReaders() throws FileNotFoundException {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();

        // get location of the plugins package
        URL rsc = cl.getResource("reader/plugins");
        if (rsc == null)
            throw new FileNotFoundException("Could not locate plugins directory.");

        File pluginsdir = new File(rsc.getFile());

        // make sure the location is a valid directory
        if (!pluginsdir.exists() || !pluginsdir.isDirectory())
            throw new FileNotFoundException("Could not locate plugins directory.");

        // createEZID a simple filter to only look at compiled class files
        FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                // only accept class files, but ignore the interface
                return (name.endsWith(".class") && !name.endsWith("TabularDataReader.class"));
            }
        };
        File[] classfiles = pluginsdir.listFiles(filter);

        String classname;
        Class newclass;
        Object newreader;

        // process each class file
        for (File classfile : classfiles) {
            classname = classfile.getName();
            classname = classname.substring(0, classname.length() - 6);
            //System.out.println(classname);

            try {
                // load the class file and instantiate the class
                newclass = cl.loadClass("reader.plugins." + classname);
                newreader = newclass.newInstance();

                // make sure this class implements the reader interface
                if (newreader instanceof TabularDataReader) {
                    // add it to the list of valid readers
                    readers.add((TabularDataReader) newreader);
                }
            } catch (Exception e) {
                System.out.println(e);
            }
        }
    }

    /**
     * Get all file formats supported by the loaded reader plugins.  The
     * returned strings contain short format identifiers that can be used to
     * request specific readers from the getReader() method.
     *
     * @return An array of file format identifiers for all loaded plugins.
     */
    public String[] getSupportedFormats() {
        String[] formats = new String[readers.size()];

        for (int cnt = 0; cnt < readers.size(); cnt++)
            formats[cnt] = readers.get(cnt).getFormatString();

        return formats;
    }

    /**
     * Get a reader for the specified input file format.  The format string
     * should correspond to the value returned by the getFormatString() method
     * of the reader.  If a reader matching the file format is found, a new
     * instance of the reader is created and returned.
     *
     * @param formatstring The input file format.
     * @return A new instance of the reader supporting the specified format.
     */
    public TabularDataReader getReader(String formatstring) {
        for (TabularDataReader reader : readers) {
            if (reader.getFormatString().equals(formatstring)) {
                try {
                    return reader.getClass().newInstance();
                } catch (Exception e) {
                    return null;
                }
            }
        }

        return null;
    }

    /**
     * Returns an iterator for all reader plugins loaded by this ReaderManager.
     * This should only be used for querying properties of the readers, and not
     * for actually obtaining a reader to use for reading data.  For that
     * purpose, use either getReader() or one of the openFile() methods, since
     * these actually return a new instance of the requested reader.
     *
     * @return An iterator for all loaded reader plugins.
     */
    @Override
    public Iterator<TabularDataReader> iterator() {
        return new ReaderIterator(readers);
    }

    /**
     * Attempts to open the specified file with an appropriate reader plugin.
     * The testFile() method of the readers is used to find a reader that can
     * open the file.  If a reader for the file type is found, a new instance
     * of the reader is created and returned after opening the file.
     *
     * @param filepath The path of the data file to open.
     * @return A new instance of a reader if an appropriate reader is found that
     *         opens the file successfully. Otherwise, returns null.
     */
    public TabularDataReader openFile(String filepath) {
        // Check all readers to see if we have one that can read the
        // specified file.
        for (TabularDataReader reader : readers) {
            if (reader.testFile(filepath)) {
                try {
                    // A matching reader was found, so create a new instance of
                    // the reader, open the file with it, and return it.
                    TabularDataReader newreader = reader.getClass().newInstance();
                    newreader.openFile(filepath);

                    return newreader;
                } catch (Exception e) {
                    return null;
                }
            }
        }

        // no matching reader was found
        return null;
    }

    /**
     * Attempts to open a data file with a specified format.  If a reader
     * supporting the format is found, a new instance of the reader is created
     * and returned after opening the file.
     *
     * @param filepath     The path of the data file to open.
     * @param formatstring The format of the input file.
     * @return A new instance of a reader if a reader for the format is
     *         available and opens the file successfully. Otherwise, returns null.
     */
    public TabularDataReader openFile(String filepath, String formatstring) {
        // get the reader for the specified file format
        TabularDataReader reader = getReader(formatstring);

        if (reader != null) {
            // if a matching reader was found, try to open the specified file
            if (reader.openFile(filepath)) {
                return reader;
            } else {
                return null;
            }
        } else {
            // no matching reader was found
            return null;
        }
    }


    /**
     * An iterator for all reader plugins loaded by the ReaderManager.  Note
     * that as ReaderManager is currently implemented, defining a separate class
     * here for an iterator over all readers is not strictly necessary.  We
     * could have just as easily returned an iterator for the list used by
     * ReaderManager to keep track of loaded plugins.  The advantage of
     * explicitly defining an iterator class here is that it gives us more
     * flexibility if the ReaderManager implementation changes in the future or
     * if we need to do more complex things within the iterator itself.
     */
    private class ReaderIterator implements Iterator<TabularDataReader> {
        private LinkedList<TabularDataReader> readerlist;
        private int index;

        public ReaderIterator(LinkedList<TabularDataReader> readerlist) {
            this.readerlist = readerlist;
            index = 0;
        }

        @Override
        public boolean hasNext() {
            return index < readerlist.size();
        }

        @Override
        public TabularDataReader next() {
            if (hasNext()) {
                index++;
                return readerlist.get(index - 1);
            } else
                throw new NoSuchElementException();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

    }
}
