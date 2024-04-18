package de.uni_mannheim.swt.lasso.srm.olap;

import org.apache.arrow.adapter.jdbc.ArrowVectorIterator;
import org.apache.arrow.adapter.jdbc.JdbcToArrowConfig;
import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.vector.VectorSchemaRoot;
import org.apache.arrow.vector.ipc.ArrowReader;
import org.apache.arrow.vector.types.pojo.Schema;

import java.io.IOException;

// copied from here https://raw.githubusercontent.com/davisusanibar/java-python-by-cdata/main/src/main/java/org/example/cdata/ShareArrowReaderAPI.java
// based on https://github.com/apache/arrow-cookbook/pull/316
public class JdbcReader extends ArrowReader {
    private final ArrowVectorIterator iter;
    private final JdbcToArrowConfig config;
    private VectorSchemaRoot root;
    private boolean firstRoot = true;

    public JdbcReader(BufferAllocator allocator, ArrowVectorIterator iter, JdbcToArrowConfig config) {
        super(allocator);
        this.iter = iter;
        this.config = config;
    }

    @Override
    public boolean loadNextBatch() throws IOException {
        if (firstRoot) {
            firstRoot = false;
            return true;
        }
        else {
            if (iter.hasNext()) {
                if (root != null && !config.isReuseVectorSchemaRoot()) {
                    root.close();
                }
                else {
                    root.allocateNew();
                }
                root = iter.next();
                return root.getRowCount() != 0;
            }
            else {
                return false;
            }
        }
    }

    @Override
    public long bytesRead() {
        return -666;
    }

    @Override
    protected void closeReadSource() throws IOException {
        if (root != null && !config.isReuseVectorSchemaRoot()) {
            root.close();
        }
    }

    @Override
    protected Schema readSchema() throws IOException {
        return null;
    }

    @Override
    public VectorSchemaRoot getVectorSchemaRoot() throws IOException {
        if (root == null) {
            root = iter.next();
        }

        return root;
    }

    @Override
    public void close() throws IOException {
        super.close();
    }
}
