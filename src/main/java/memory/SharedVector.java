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
        // We use a read lock so no one can modify the vector while we're reading
        readLock();
        try {
            return vector[index];
        } finally {
            readUnlock();
        }
    }

    public int length() {
        // Read lock - prevents other threads from changing the vector while we check its size
        readLock();
        try {
            return vector.length;
        } finally {
            readUnlock();
        }
    }

    public VectorOrientation getOrientation() {
        // TODO: return vector orientation
        // Read lock - ensures we see the current orientation (might change during transpose)
        readLock();
        try {
            return orientation;
        } finally {
            readUnlock();
        }
    }

    public void writeLock() {
        // TODO: acquire write lock
        // Write lock = exclusive access. Only one thread can hold it, and no readers allowed
        this.lock.writeLock().lock();
    }

    public void writeUnlock() {
        // TODO: release write lock
        this.lock.writeLock().unlock();
    }

    public void readLock() {
        // TODO: acquire read lock
        // Read lock = shared access. Multiple readers can hold it, but blocks writers
        this.lock.readLock().lock();
    }

    public void readUnlock() {
        // TODO: release read lock
        this.lock.readLock().unlock();
    }

    public void transpose(){
        // TODO: transpose vector
        writeLock();
        try {
            VectorOrientation curr = this.orientation;
            this.orientation = curr == VectorOrientation.COLUMN_MAJOR ? VectorOrientation.ROW_MAJOR : VectorOrientation.COLUMN_MAJOR;
        } finally {
            writeUnlock();
        }
    }

    public void add(SharedVector other) {
        // TODO: add two vectors
        // Write lock on 'this' because we modify it, read lock on 'other' because we only read it
        // No deadlock here - the engine always locks left operand first, so lock order is consistent
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
        // Write lock - we're modifying the vector, so no one else should read/write during this
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
        // Read locks on both vectors - we're only reading, so multiple threads can do this at once
        this.readLock();
        other.readLock();
        double dotRes = 0;
        try {
            if (this.vector.length != other.vector.length) {
                throw new IllegalArgumentException("Vectors' lengths does not match");
            }
            if (this.orientation == other.orientation || this.orientation != VectorOrientation.ROW_MAJOR) {
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
        // Assumes this vector is ROW_MAJOR and matrix is COLUMN_MAJOR
        // We compute the result first, then use write lock to replace the vector atomically
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
