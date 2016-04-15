package cz.d1x.dxutils;

import java.io.*;

/**
 * Implementation of {@link DataStorage} that stores data in-memory as long as they does not grow over given
 * size threshold. Once the threshold is reached, the data gets automatically serialized to the backing file.
 * <p>
 * The implementation is not thread-safe. It is up to client to take care of synchronization on single write/read
 * operations if needed.
 */
public class MemoryFileBackedStorage implements DataStorage {

    private static final long DEFAULT_THRESHOLD = 5 * 1000 * 1000; // 5MB

    private final long sizeThreshold;
    private final File backingFile;

    private ByteArrayOutputStream memoryBytes = new ByteArrayOutputStream();
    private OutputStream fileOutputStream = null;

    /**
     * Creates a new storage with given backing file and default threshold of 5MB.
     *
     * @param backingFile file to be used for storing bytes if size exceeds 5MB
     */
    public MemoryFileBackedStorage(File backingFile) {
        this(DEFAULT_THRESHOLD, backingFile);
    }

    /**
     * Creates a new storage with given backing file and given threshold.
     *
     * @param sizeThreshold size threshold (in bytes) to drive switching to memory.
     * @param backingFile   file to be used for storing bytes if size exceeds threshold
     */
    public MemoryFileBackedStorage(long sizeThreshold, File backingFile) {
        if (sizeThreshold < 0) {
            throw new IllegalArgumentException("Must specify non-negative threshold value");
        }
        this.sizeThreshold = sizeThreshold;
        this.backingFile = backingFile;
    }

    /**
     * Gets output stream for writing data to the storage.
     * The stream automatically starts writing to memory and switches to file if threshold is reached.
     * Note that client should close the stream when finished with writing to it.
     * <p>
     * If client calls this method and storage already contains any data, these data will be automatically destroyed by
     * {@link #destroy()} to avoid unpredicted state.
     *
     * @return output stream for writing data to the storage.
     */
    public OutputStream getOutputStream() {
        destroy(); // clear internal state
        return new OutputStream() {

            @Override
            public void write(int b) throws IOException {
                if (memoryBytes.size() + 1 >= sizeThreshold) {
                    copyMemoryToFile();
                    fileOutputStream.write(b);
                } else {
                    memoryBytes.write(b);
                }
            }

            @Override
            public void write(byte[] b) throws IOException {
                if (memoryBytes.size() + b.length >= sizeThreshold) {
                    copyMemoryToFile();
                    fileOutputStream.write(b);
                } else {
                    memoryBytes.write(b);
                }
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                if (memoryBytes.size() + len >= sizeThreshold) {
                    copyMemoryToFile();
                    fileOutputStream.write(b, off, len);
                } else {
                    memoryBytes.write(b, off, len);
                }
            }

            @Override
            public void close() throws IOException {
                if (fileOutputStream != null) {
                    fileOutputStream.close();
                }
            }
        };
    }

    /**
     * Gets input stream for reading data from the storage.
     * The stream automatically reads from memory or file depending on the data in the storage.
     * Note that client should close the stream when finished reading from it.
     * <p>
     * Clients can create as many input streams as they want.
     * If data are held in memory, the reading should not interfere. If they are held in the backing file, concurrent
     * reading is driven by OS-specific {@link FileInputStream} implementation.
     *
     * @return input stream for reading data from the storage
     */
    public InputStream getInputStream() {
        if (fileOutputStream != null) {
            ensureFileOutputStreamIsClosed();
            try {
                return new FileInputStream(backingFile);
            } catch (FileNotFoundException e) {
                throw new IllegalStateException("No data were written to the storage or backing file was deleted", e);
            }
        } else {
            return new ByteArrayInputStream(memoryBytes.toByteArray());
        }
    }

    /**
     * Destroys this storage by releasing memory (if any data there) and deleting backing file if there is any.
     */
    public void destroy() {
        memoryBytes = new ByteArrayOutputStream();
        if (fileOutputStream != null) {
            try {
                fileOutputStream.close();
            } catch (IOException e) {
                // no-op, we don't care in destroy
            }
        }
        backingFile.delete();
    }

    /**
     * Destroys this storage by releasing memory (if any data there) and deleting backing file if there is any.
     */
    public void close() {
        destroy();
    }

    private void copyMemoryToFile() throws IOException {
        fileOutputStream = new FileOutputStream(backingFile);
        fileOutputStream.write(memoryBytes.toByteArray());
        memoryBytes = new ByteArrayOutputStream(); // free memory
    }

    private void ensureFileOutputStreamIsClosed() {
        try {
            fileOutputStream.close(); // if already closed, does nothing
        } catch (IOException e) {
            throw new IllegalStateException("Output stream to backing file cannot be closed", e);
        }
    }
}
