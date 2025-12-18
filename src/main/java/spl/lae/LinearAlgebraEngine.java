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
        if (computationRoot == null) {
            throw new IllegalArgumentException("computation root should not be null");
        }
        ComputationNode res;
        if (computationRoot.getNodeType() == ComputationNodeType.MATRIX) {
            return computationRoot;
        } else {
            List<ComputationNode> tempChildrenList = computationRoot.getChildren();
            List<ComputationNode> resolvedChildrenList = new ArrayList<>();
            if(tempChildrenList == null){
                throw new IllegalArgumentException("can't apply operation, node should have children");
            }
            for(ComputationNode child : tempChildrenList){
                ComputationNode temp = run(child);
                resolvedChildrenList.add(temp);
            }
            res = new ComputationNode(computationRoot.getNodeType(), resolvedChildrenList);
            loadAndCompute(res);
            return new ComputationNode(leftMatrix.readRowMajor());
        }
    }

    public void loadAndCompute(ComputationNode node) {
        // TODO: load operand matrices
        // TODO: create compute tasks & submit tasks to executor

        //load operand matrix :
        if(node.getChildren() == null) {
            throw new IllegalArgumentException("can't compute, node have no children");
        }
        ComputationNode left;
        ComputationNode right;
        ComputationNodeType type = node.getNodeType();
        if(type == ComputationNodeType.ADD){
            if(node.getChildren().size() < 2){
                throw new IllegalArgumentException("can't add, node have less than 2 children");
            }
            left = node.getChildren().get(0);
            leftMatrix.loadColumnMajor(left.getMatrix());
            right = node.getChildren().get(1);
            rightMatrix.loadColumnMajor(right.getMatrix());
            executor.submitAll(createAddTasks());
        }else if(type == ComputationNodeType.MULTIPLY){
            if(node.getChildren().size() < 2){
                throw new IllegalArgumentException("can't multiply, node have less than 2 children");
            }
            left = node.getChildren().get(0);
            leftMatrix.loadRowMajor(left.getMatrix());
            right = node.getChildren().get(1);
            rightMatrix.loadColumnMajor(right.getMatrix());
            executor.submitAll(createMultiplyTasks());
        }else if(type == ComputationNodeType.NEGATE){
            left = node.getChildren().get(0);
            leftMatrix.loadColumnMajor(left.getMatrix());
            executor.submitAll(createNegateTasks());

        }else if(type == ComputationNodeType.TRANSPOSE){
            left = node.getChildren().get(0);
            leftMatrix.loadColumnMajor(left.getMatrix());
            executor.submitAll(createTransposeTasks());
        }

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
        return executor.getWorkerReport();
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
