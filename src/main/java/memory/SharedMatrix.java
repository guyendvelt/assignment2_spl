package memory;

public class SharedMatrix {

    private volatile SharedVector[] vectors = {}; // underlying vectors

    public SharedMatrix() {
        // TODO: initialize empty matrix
    }

    public SharedMatrix(double[][] matrix) {
        // TODO: construct matrix as row-major SharedVectors
        if (matrix == null) {
            throw new IllegalArgumentException("Matrix cannot be null");
        }
        vectors = new SharedVector[matrix.length];
        for (int i = 0; i < matrix.length; i++) {
            vectors[i] = new SharedVector(matrix[i], VectorOrientation.ROW_MAJOR);
        }
    }

    public void loadRowMajor(double[][] matrix) {
        // TODO: replace internal data with new row-major matrix
        if (matrix == null) {
            throw new IllegalArgumentException("Matrix cannot be null");
        }
        SharedVector[] tempVectors = new SharedVector[matrix.length];
        for (int i = 0; i < matrix.length; i++) {
            tempVectors[i] = new SharedVector(matrix[i], VectorOrientation.ROW_MAJOR);
        }
        vectors = tempVectors;
    }

    public void loadColumnMajor(double[][] matrix) {
        // TODO: replace internal data with new column-major matrix
        if (matrix == null) {
            throw new IllegalArgumentException("Matrix cannot be null");
        }
        if(matrix.length == 0 || matrix[0].length == 0){
            this.vectors = new SharedVector[0];
        } else {
            SharedVector[] tempVectors = new SharedVector[matrix[0].length];
            int cols = tempVectors.length;
            int rows = matrix.length;
            for (int i = 0; i < cols; i++) {
                double[] tempCol = new double[rows];
                for (int j = 0; j < rows; j++) {
                    tempCol[j] = matrix[j][i];
                }
                tempVectors[i] = new SharedVector(tempCol, VectorOrientation.COLUMN_MAJOR);
            }
            vectors = tempVectors;
        }

    }

    public double[][] readRowMajor() {
        // TODO: return matrix contents as a row-major double[][]
        SharedVector[] tempVectors = vectors;
        if (tempVectors == null || tempVectors.length == 0){
            return new double[0][0];
        }
        acquireAllVectorReadLocks(tempVectors);
        try {
            double[][] matrix;
            int cols, rows;
            boolean isRowMajor = tempVectors[0].getOrientation() == VectorOrientation.ROW_MAJOR;
            if (!isRowMajor) {
                rows = tempVectors[0].length();
                cols = tempVectors.length;
            } else {
                rows = tempVectors.length;
                cols = tempVectors[0].length();
            }
            matrix = new double[rows][cols];
            for (int i = 0; i < tempVectors.length; i++) {
                SharedVector tempVec = tempVectors[i];
                if (isRowMajor) {
                    for (int j = 0; j < tempVec.length(); j++) {
                        matrix[i][j] = tempVec.get(j);
                    }
                } else {
                    for (int j = 0; j < tempVec.length(); j++) {
                        matrix[j][i] = tempVec.get(j);
                    }
                }
            }
            return matrix;

        } finally {
            releaseAllVectorReadLocks(tempVectors);

        }
    }

    public SharedVector get(int index) {
        // TODO: return vector at index
        SharedVector[] tempVectors = vectors;
        if(index >= tempVectors.length || index < 0) {
            throw new IllegalArgumentException("index is Illegal");
        }
            return tempVectors[index];
    }

    public int length() {
        // TODO: return number of stored vectors
        return vectors == null ? 0 : vectors.length;
    }

    public VectorOrientation getOrientation() {
        // TODO: return orientation
        if(this.length() == 0) {
            throw new IllegalArgumentException("matrix is empty, no orientation exists");
        }
        return this.vectors[0].getOrientation();
    }

    private void acquireAllVectorReadLocks(SharedVector[] vecs) {
        // TODO: acquire read lock for each vector
        for (SharedVector v : vecs) {
            v.readLock();
        }
    }

    private void releaseAllVectorReadLocks(SharedVector[] vecs) {
        // TODO: release read locks
        for (SharedVector v : vecs) {
            v.readUnlock();
        }
    }

    private void acquireAllVectorWriteLocks(SharedVector[] vecs) {
        // TODO: acquire write lock for each vector
        for (SharedVector v : vecs) {
            v.writeLock();
        }
    }

    private void releaseAllVectorWriteLocks(SharedVector[] vecs) {
        // TODO: release write locks
        for (SharedVector v : vecs) {
            v.writeUnlock();
        }
    }
}
