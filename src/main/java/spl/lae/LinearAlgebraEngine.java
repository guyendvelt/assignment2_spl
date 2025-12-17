package spl.lae;

import parser.*;
import memory.*;
import scheduling.*;

import java.util.ArrayList;
import java.util.List;

public class LinearAlgebraEngine {

    private SharedMatrix leftMatrix = new SharedMatrix();
    private SharedMatrix rightMatrix = new SharedMatrix();
    private TiredExecutor executor;

    public LinearAlgebraEngine(int numThreads) {
        // TODO: create executor with given thread count
        executor = new TiredExecutor(numThreads);
    }

    public ComputationNode run(ComputationNode computationRoot) {
        // TODO: resolve computation tree step by step until final matrix is produced\
        ComputationNode res;
        if (computationRoot.getNodeType() == ComputationNodeType.MATRIX) {
            res = new ComputationNode(computationRoot.getMatrix());
            return res;

        } else if (computationRoot.getNodeType() == ComputationNodeType.NEGATE) {
            if (computationRoot.getChildren().size() != 1) {
                throw new ArithmeticException("Negate is an Unary function");
            }
            ComputationNode recursiveNode = run(computationRoot.getChildren().get(0));
            createNegateTasks()
            double[][] currMatrix = recursiveNode.getMatrix();
            SharedMatrix tempMatrix = new SharedMatrix(currMatrix);
            for (int i = 0; i < currMatrix.length; i++) {
                tempMatrix.get(i).negate();
            }
            currMatrix = tempMatrix.readRowMajor();
            return run(new ComputationNode(currMatrix));
        }
    }

    public void loadAndCompute(ComputationNode node) {
        // TODO: load operand matrices
        // TODO: create compute tasks & submit tasks to executor
    }

    public List<Runnable> createAddTasks() {
        // TODO: return tasks that perform row-wise addition
        if (leftMatrix == null || rightMatrix == null || leftMatrix.length() == 0 || rightMatrix.length() == 0) {
            throw new IllegalArgumentException("can't add empty Matrix");
        }
        List<Runnable> tasks = new ArrayList<>();
        if (!canAdd()) {
            throw new IllegalArgumentException("can't add matrix with different dimensions");
        }
        if (leftMatrix.getOrientation() != rightMatrix.getOrientation()) {
            SharedMatrix matrixToChange;
            if (leftMatrix.getOrientation() == VectorOrientation.COLUMN_MAJOR) {
                matrixToChange = leftMatrix;
            } else {
                matrixToChange = rightMatrix;
            }
            double[][] temp = matrixToChange.readRowMajor();
            matrixToChange.loadColumnMajor(temp);
        }
        for (int i = 0; i < leftMatrix.length(); i++) {
            final int row = i;
            tasks.add(() -> {
                leftMatrix.get(row).add(rightMatrix.get(row));
            });
        }
        return tasks;
    }

    public List<Runnable> createMultiplyTasks() {
        // TODO: return tasks that perform row × matrix multiplication
        if (leftMatrix == null || rightMatrix == null || leftMatrix.length() == 0 || rightMatrix.length() == 0) {
            throw new IllegalArgumentException("can't multiply empty Matrix");
        }
        if(!canMultiply()){
            throw new ArithmeticException("Can't multiply matrix with different dimensions");
        }
        if (leftMatrix.getOrientation() != VectorOrientation.ROW_MAJOR) {
            double[][] temp = leftMatrix.readRowMajor();
            leftMatrix.loadRowMajor(temp);
        }
        if (rightMatrix.getOrientation() != VectorOrientation.COLUMN_MAJOR) {
            double[][] temp = rightMatrix.readRowMajor();
            rightMatrix.loadColumnMajor(temp);
        }
        List<Runnable> tasks = new ArrayList<>();
        for (int i = 0; i < leftMatrix.length(); i++) {
            final int row = i;
            tasks.add(() -> {
                leftMatrix.get(row).vecMatMul(rightMatrix);
            });
        }
        return tasks;
    }

    public List<Runnable> createNegateTasks() {
        // TODO: return tasks that negate rows
        List<Runnable> tasks = new ArrayList<>();
        if (leftMatrix == null || leftMatrix.length() == 0) {
            throw new IllegalArgumentException("can't negate empty Matrix");
        }
        for (int i = 0; i < leftMatrix.length(); i++) {
            final int row = i;
            tasks.add(() -> {
                leftMatrix.get(row).negate();
            });
        }
        return tasks;
    }

    public List<Runnable> createTransposeTasks() {
        // TODO: return tasks that transpose rows
        List<Runnable> tasks = new ArrayList<>();
        if (leftMatrix == null || leftMatrix.length() == 0) {
            throw new IllegalArgumentException("can't transpose empty Matrix");
        }
        for (int i = 0; i < leftMatrix.length(); i++) {
            final int row = i;
            tasks.add(() -> {
                leftMatrix.get(row).transpose();
            });
        }
        return tasks;
    }

    public String getWorkerReport() {
        // TODO: return summary of worker activity
        return null;
    }

    private boolean canAdd() {
        if (leftMatrix.getOrientation() == rightMatrix.getOrientation()) {
            return leftMatrix.length() == rightMatrix.length()
                    && leftMatrix.get(0).length() == rightMatrix.get(0).length();
        } else {
            return leftMatrix.length() == rightMatrix.get(0).length()
                    && leftMatrix.get(0).length() == rightMatrix.length();
        }
    }

    private boolean canMultiply(){
        VectorOrientation leftOrientation = leftMatrix.getOrientation();
        VectorOrientation rightOrientation = rightMatrix.getOrientation();
        if(leftOrientation == rightOrientation){
            if(leftOrientation == VectorOrientation.ROW_MAJOR){
                return leftMatrix.get(0).length() == rightMatrix.length();
            } else {
                return leftMatrix.length() == rightMatrix.get(0).length();
            }
        } else {
            if(leftOrientation == VectorOrientation.ROW_MAJOR){
                return leftMatrix.get(0).length() == rightMatrix.get(0).length();
            } else {
                return leftMatrix.length() == rightMatrix.length();
            }
        }
    }


}
