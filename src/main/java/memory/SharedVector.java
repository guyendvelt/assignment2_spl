package memory;

import java.util.concurrent.locks.ReadWriteLock;

public class SharedVector {

    private double[] vector;
    private VectorOrientation orientation;
    private ReadWriteLock lock = new java.util.concurrent.locks.ReentrantReadWriteLock();

    public SharedVector(double[] vector, VectorOrientation orientation) {
        // TODO: store vector data and its orientation
        this.vector = vector;
        this.orientation = orientation;
    }

    public double get(int index) {
        // TODO: return element at index (r ead-locked)
        readLock();
        try {
            return vector[index];
        } finally {
            readUnlock();
        }
    }

    public int length() {
        readLock();
        try {
            return vector.length;
        } finally {
            readUnlock();
        }
    }

    public VectorOrientation getOrientation() {
        // TODO: return vector orientation
        readLock();
        try {
            return orientation;
        } finally {
            readUnlock();
        }
    }

    public void writeLock() {
        // TODO: acquire write lock
        this.lock.writeLock().lock();

    }

    public void writeUnlock() {
        // TODO: release write lock
        this.lock.writeLock().unlock();
    }

    public void readLock() {
        // TODO: acquire read lock
        this.lock.readLock().lock();
    }

    public void readUnlock() {
        // TODO: release read lock
        this.lock.readLock().unlock();
    }

    public void transpose() {
        // TODO: transpose vector
        writeLock();
        try {
            VectorOrientation curr = this.orientation;
            if (curr == VectorOrientation.ROW_MAJOR) {
                this.orientation = VectorOrientation.COLUMN_MAJOR;
            } else {
                this.orientation = VectorOrientation.ROW_MAJOR;
            }
        } finally {
            writeUnlock();
        }
    }

    public void add(SharedVector other) {
        // TODO: add two vectors
        this.writeLock();
        other.readLock();
        try {
            if (this.vector.length != other.vector.length) {
                throw new IllegalArgumentException("Vectors' lengths does not match");
            } else if (this.orientation != other.orientation) {
                throw new IllegalArgumentException("Orientations does not match");
            }
            for (int i = 0; i < this.vector.length; i++) {
                this.vector[i] += other.vector[i];
            }
        } finally {
            other.readUnlock();
            this.writeUnlock();
        }
    }

    public void negate() {
        // TODO: negate vector
        this.writeLock();
        try {
            for (int i = 0; i < vector.length; i++) {
                this.vector[i] = 0 - this.vector[i];
            }
        } finally {
            this.writeUnlock();
        }
    }

    public double dot(SharedVector other) {
        // TODO: compute dot product (row · column)
        this.readLock();
        other.readLock();
        double dotRes = 0;
        try {
            if (this.vector.length != other.vector.length) {
                throw new IllegalArgumentException("Vectors' lengths does not match");
            } else if (this.orientation == other.orientation) {
                throw new IllegalArgumentException("Orientations does not match");
            }
            for (int i = 0; i < this.vector.length; i++) {
                dotRes += this.vector[i] * other.vector[i];
            }
            return dotRes;
        } finally {
            other.readUnlock();
            this.readUnlock();
        }

    }

    public void vecMatMul(SharedMatrix matrix) {
        // TODO: compute row-vector × matrix
        if (matrix == null) {
            throw new IllegalArgumentException("matrix cannot be null");
        }
        if (this.getOrientation() == VectorOrientation.COLUMN_MAJOR) {
            throw new IllegalArgumentException("vector should not be column major");
        }

        double[] result;
        SharedVector tempVector;
        if (this.length() != matrix.get(0).length()) {
            throw new IllegalArgumentException("Dimensions mismatch");
        }
        result = new double[matrix.length()];
        for (int i = 0; i < result.length; i++) {
            result[i] = this.dot(matrix.get(i));
        }
        tempVector = new SharedVector(result, VectorOrientation.ROW_MAJOR);
        writeLock();
        try {
            this.vector = tempVector.vector;
        } finally {
            writeUnlock();
        }
    }
}
