package com.gudong.action;

import com.gudong.utils.LogUtils;
import com.gudong.utils.MoExpressionUtils;
import com.gudong.utils.NotifyUtils;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.JavaRecursiveElementVisitor;
import com.intellij.psi.PsiAssignmentExpression;
import com.intellij.psi.PsiCatchSection;
import com.intellij.psi.PsiCodeBlock;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiExpressionStatement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiIfStatement;
import com.intellij.psi.PsiKeyword;
import com.intellij.psi.PsiLocalVariable;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiReturnStatement;
import com.intellij.psi.PsiStatement;
import com.intellij.psi.PsiThrowStatement;
import com.intellij.psi.PsiTryStatement;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiWhileStatement;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.util.Query;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * description 修改代码
 *
 * @author maggie
 * @date 2021-08-29 13:22
 */
public class FixCode {


    public static void scan(Project project, Collection<VirtualFile> files) {
     /*   ThreadPoolExecutor executor = new ThreadPoolExecutor(16, 100, 200, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(files.size()));*/
     //   CountDownLatch latch = new CountDownLatch(files.size());
        final float[] all = {0f};
        for (VirtualFile file : files) {
            ApplicationManager.getApplication().runWriteAction(new Runnable() {
                @Override
                public void run() {
                    try {
                        scanCode(project, PsiManager.getInstance(project).findFile(file));
                    } catch (Exception e) {
                        e.printStackTrace();
                        LogUtils.error(file.getName(), e);
                    } finally {
                 //       latch.countDown();
                    }
                }
            });
           /* try {
                scanCode(project, PsiManager.getInstance(project).findFile(file));
            } catch (Exception e) {
                e.printStackTrace();
                LogUtils.error(file.getName(), e);
            }*/
        /*    executor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        scanCode(project, PsiManager.getInstance(project).findFile(file));
                        LogUtils.info("线程池中线程数目：" + executor.getPoolSize() + "，队列中等待执行的任务数目：" +
                                executor.getQueue().size() + "，已执行完的任务数目：" + executor.getCompletedTaskCount());
                    } catch (Exception e) {
                        e.printStackTrace();
                        LogUtils.error(file.getName(), e);
                    } finally {

                        latch.countDown();
                    }
                }
            });*/
         /*   ProgressManager.getInstance().run(new Task.Backgroundable(project, "Title"){
                @Override
                public void run(@NotNull ProgressIndicator progressIndicator) {

                    // start your process
                    try {
                        scanCode(project, PsiManager.getInstance(project).findFile(file));
                        all[0] +=1/files.size();
                        // Set the progress bar percentage and text
                        progressIndicator.setFraction(all[0]);
                        progressIndicator.setText("checking...");
                        LogUtils.info(""+all[0]);
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        latch.countDown();
                    }

                }
             });*/

        }
        //阻塞统计方法：采用countlatch的递减方法，同时执行await()方法进行阻塞，每一个线程执行完就进行递减，直到为0才继续执行最终的统计输出代码
       /* try {
            //阻塞，直到latch为0才执行下面的输出语句
            latch.await();
            LogUtils.info("所有线程执行完毕！");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/
        //    executor.shutdown();
    }

    private static void scanCode(Project project, PsiFile psiFile) throws Exception {
        final int[] notCloseCount = {0, 0};
        final int[] closeCount = {0, 0};
        LogUtils.info(psiFile.getName() + " start checking");
        psiFile.accept(new JavaRecursiveElementVisitor() {
            @Override
            public void visitLocalVariable(PsiLocalVariable variable) {
                PsiType localVariableType = variable.getType();
                //java.sql.Statement PreparedStatement java.sql.ResultSet
                if ("java.sql.PreparedStatement".equals(localVariableType.getCanonicalText())) {
                    // super.visitLocalVariable(variable);
                    final Query<PsiReference> search = ReferencesSearch.search(variable);
                    final Collection<PsiReference> all = search.findAll();
                    //是否关闭资源了
                    final boolean[] isClose = {false};
                    //是否有catch块了
                    boolean isCatch = false;
                    PsiCatchSection catchSection = null;
                    int i = 0;
                    for (PsiReference psiReference : all) {
                        //   //LogUtils.debug("pst:" + psiReference);
                        PsiStatement currElem = MoExpressionUtils.getParentOfStatement(psiReference.getElement());
                        if (currElem instanceof PsiExpressionStatement &&
                                ((PsiExpressionStatement) currElem).getExpression() instanceof PsiMethodCallExpression) {
                            if (MoExpressionUtils.hasFullQualifiedName((PsiMethodCallExpression) ((PsiExpressionStatement) currElem).getExpression(), "java.sql.Connection", "prepareStatement")) {
                                if (i != 0) {
                                    if (!isClose[0]) {
                                        //LogUtils.debug("有一次stmt没关");
                                        notCloseCount[0]++;
                                    } else {
                                        isClose[0] = false;
                                    }
                                }
                            } else if (MoExpressionUtils.hasFullQualifiedName((PsiMethodCallExpression) ((PsiExpressionStatement) currElem).getExpression(), "java.sql.Statement", "close")) {
                             //   System.out.println("cur"+currElem.getText());
                                PsiElement parent = currElem.getParent().getParent();
                            //    System.out.println("parent"+parent.getText());
                                //判断是否 在catch里关掉的，如果是则无效
                                if (parent instanceof PsiCatchSection) {
                                    isCatch = true;
                                    catchSection = (PsiCatchSection) parent;
                                } else {
                                    //判断是否 在if里关掉的，如果是则检查else关闭了吗,没关在else里关掉
                                    // 注意：if块是包含else块的
                                    // 如果是在else里关的，而且有throw或return 那就正常，但是要在if 的then块或if块的后面还有close语句 才是两个分支都关了
                                    // TODO 其实这样也是不对的，最好是得到方法内所有的return和throw，判断所有分支可能性都要close，不过还是先这样吧
                                    final PsiIfStatement parentOfIf = MoExpressionUtils.getParentOfIf(parent);
                                    if (parentOfIf instanceof PsiIfStatement) {
                                        final PsiExpression condition = parentOfIf.getCondition();
                                        final PsiElement firstChild = parentOfIf.getElseBranch().getFirstChild();
                                        if (condition instanceof PsiMethodCallExpression &&
                                                MoExpressionUtils.hasFullQualifiedName((PsiMethodCallExpression) condition, "java.sql.ResultSet", "next"
                                                ) && firstChild != null) {
                                            final PsiElement[] children = firstChild.getChildren();
                                            for (PsiElement child : children) {
                                              //  System.out.println(child.getText());
                                                if(child == currElem){
                                                    System.out.println("true");
                                                }

                                              /*  if ((child instanceof PsiExpressionStatement &&
                                                        ((PsiExpressionStatement) child).getExpression() instanceof PsiReturnStatement)
                                                        || child instanceof PsiThrowStatement) {

                                                }*/
                                            }
                                  /*          notCloseCount[0]++;
                                            WriteCommandAction.runWriteCommandAction(project, new Runnable() {
                                                @Override
                                                public void run() {
                                                    PsiElementFactory factory = JavaPsiFacade.getInstance(project).getElementFactory();
                                                    final PsiElement[] children2 = elseBranch.getFirstChild().getChildren();
                                                    for (PsiElement child : children2) {
                                                        if ((child instanceof PsiExpressionStatement &&
                                                                ((PsiExpressionStatement) child).getExpression() instanceof PsiReturnStatement)
                                                                || child instanceof PsiThrowStatement) {
                                                            child.getParent().addBefore(factory.createStatementFromText(variable.getName() + ".close();", null), child);
                                                            closeCount[0]++;
                                                            isClose[0] = true;
                                                        }
                                                    }
                                                }
                                            });*/
                                            isClose[0] = true;
                                        } else {
                                            isClose[0] = true;
                                        }

                                    } else {
                                        isClose[0] = true;
                                    }
                                }
                            } else if (MoExpressionUtils.hasFullQualifiedName((PsiMethodCallExpression) ((PsiExpressionStatement) currElem).getExpression(), "com.cw.wizbank.util.cwSQL", "cleanUp")
                                    || MoExpressionUtils.hasFullQualifiedName((PsiMethodCallExpression) ((PsiExpressionStatement) currElem).getExpression(), "com.cw.wizbank.util.cwSQL", "closePreparedStatement")) {
                                isClose[0] = true;
                            }
                        }
                        i++;
                    }
                    if (!isClose[0]) {
                        //LogUtils.debug("最后一次stmt没关");
                        notCloseCount[0]++;
                        PsiCatchSection finalCatchSection2 = catchSection;
                        WriteCommandAction.runWriteCommandAction(project, new Runnable() {
                            @Override
                            public void run() {
                                PsiElementFactory factory = JavaPsiFacade.getInstance(project).getElementFactory();

                                //有catch就在finally给他关了(前提存在finally)，-没有就在方法最后关掉 -- 暂弃：没有就建try/catch关掉
                                if (finalCatchSection2 != null) {
                                    PsiCatchSection finalCatchSection = finalCatchSection2;
                                    PsiKeyword keyword = factory.createKeyword(PsiKeyword.FINALLY);
                                    PsiCodeBlock codeBlock = factory.createCodeBlockFromText("{cwSQL.cleanUp(null, " + variable.getName() + ");}", null);
                                    finalCatchSection.addAfter(keyword, finalCatchSection);
                                    finalCatchSection.addAfter(codeBlock, finalCatchSection);
                                    closeCount[0]++;
                                } else {
                                    PsiMethod method = MoExpressionUtils.getParentOfMethod(variable);
                                    PsiCodeBlock body = method.getBody();
                                    PsiStatement[] statements = body.getStatements();
                                    PsiStatement last = statements[statements.length - 1];
                                    PsiStatement statementFromText = factory.createStatementFromText(variable.getName() + ".close();", null);
                                    if (last instanceof PsiReturnStatement) {
                                        last.getParent().addBefore(statementFromText, last);
                                    } else {
                                        last.getParent().addAfter(statementFromText, last);
                                    }
                                    closeCount[0]++;
                                }
                            }
                        });
                    }
                }
                if ("java.sql.ResultSet".equals(localVariableType.getCanonicalText())) {
                    final Query<PsiReference> search = ReferencesSearch.search(variable);
                    final Collection<PsiReference> all = search.findAll();
                    //是否关闭资源了
                    boolean isClose = false;
                    //是否有catch块了
                    boolean isCatch = false;
                    PsiCatchSection catchSection = null;
                    //存最后一次位置引用
                    PsiReference lastRef = null;
                    int i = 0;
                    for (PsiReference psiReference : all) {
                        lastRef = psiReference;
                        PsiStatement currElem = MoExpressionUtils.getParentOfStatement(psiReference.getElement());
                        if (currElem instanceof PsiExpressionStatement &&
                                ((PsiExpressionStatement) currElem).getExpression() instanceof PsiMethodCallExpression) {
                            if (MoExpressionUtils.hasFullQualifiedName((PsiMethodCallExpression) ((PsiExpressionStatement) currElem).getExpression(), "java.sql.PreparedStatement", "executeQuery")) {
                                if (i != 0) {
                                    if (!isClose) {
                                        //LogUtils.debug("有一次rs没关");
                                        notCloseCount[1]++;
                                    } else {
                                        isClose = false;
                                    }
                                }
                            } else if (MoExpressionUtils.hasFullQualifiedName((PsiMethodCallExpression) ((PsiExpressionStatement) currElem).getExpression(), "java.sql.ResultSet", "close")) {
                                final PsiElement parent = currElem.getParent().getParent();
                                //判断是否 在catch里关掉的，如果是则无效
                                if (!(parent instanceof PsiCatchSection)) {
                                    isClose = true;
                                } else {
                                    isCatch = true;
                                    catchSection = (PsiCatchSection) parent;
                                }
                            } else if (MoExpressionUtils.hasFullQualifiedName((PsiMethodCallExpression) ((PsiExpressionStatement) currElem).getExpression(), "com.cw.wizbank.util.cwSQL", "cleanUp") ||
                                    MoExpressionUtils.hasFullQualifiedName((PsiMethodCallExpression) ((PsiExpressionStatement) currElem).getExpression(), "com.cw.wizbank.util.cwSQL", "closeResultSet")) {
                                isClose = true;
                            }
                        }
                        i++;

                    }
                    if (!isClose) {
                        //LogUtils.debug("最后一次rs没关");
                        notCloseCount[1]++;
                        PsiReference finalLastRef2 = lastRef;
                        WriteCommandAction.runWriteCommandAction(project, new Runnable() {
                            @Override
                            public void run() {
                                PsiElementFactory factory = JavaPsiFacade.getInstance(project).getElementFactory();
                                //判断是不是在if/while里关 , 最后判断try有没有，没有就拿方法，有就再判断finally
                                PsiReference finalLastRef = finalLastRef2;
                                final PsiStatement psiStatement = MoExpressionUtils.getParentOfIfOrWhile(finalLastRef2.getElement());
                                if (psiStatement != null) {
                                    if (psiStatement instanceof PsiIfStatement) {
                                        PsiIfStatement ifStatement = (PsiIfStatement) psiStatement;
                                        final PsiStatement thenBranch = ifStatement.getThenBranch();
                                        final PsiStatement elseBranch = ifStatement.getElseBranch();
                                        final PsiElement[] children = thenBranch.getFirstChild().getChildren();
                                        boolean lflag = false, rflag = false;
                                        for (PsiElement child : children) {
                                            if ((child instanceof PsiExpressionStatement &&
                                                    ((PsiExpressionStatement) child).getExpression() instanceof PsiReturnStatement)
                                                    || child instanceof PsiThrowStatement) {
                                                child.getParent().addBefore(factory.createStatementFromText(finalLastRef.getCanonicalText() + ".close();", null), child);
                                                lflag = true;
                                            }
                                        }
                                        if (elseBranch != null) {
                                            final PsiElement[] children2 = elseBranch.getFirstChild().getChildren();
                                            for (PsiElement child : children2) {
                                                if ((child instanceof PsiExpressionStatement &&
                                                        ((PsiExpressionStatement) child).getExpression() instanceof PsiReturnStatement)
                                                        || child instanceof PsiThrowStatement) {
                                                    child.getParent().addBefore(factory.createStatementFromText(finalLastRef.getCanonicalText() + ".close();", null), child);
                                                    rflag = true;
                                                }
                                            }
                                            if (rflag && !lflag) {
                                                FixCode.fixBranch(factory, thenBranch, finalLastRef);
                                            }
                                        }
                                        //如果只有if 又没return，只能在最后那关 如果else有throw,那么要在throw前关掉

                                        if (!rflag && !lflag) {
                                            ifStatement.getParent().addAfter(factory.createStatementFromText(finalLastRef.getCanonicalText() + ".close();", null), ifStatement);
                                        }
                                        closeCount[1]++;
                                    } else if (psiStatement instanceof PsiWhileStatement) {
                                        PsiWhileStatement whileStatement = (PsiWhileStatement) psiStatement;
                                        whileStatement.getParent().addAfter(factory.createStatementFromText(finalLastRef.getCanonicalText() + ".close();", null), whileStatement);
                                        closeCount[1]++;
                                    }
                                } else {
                                    final PsiTryStatement parentOfTry = MoExpressionUtils.getParentOfTry(finalLastRef2.getElement());
                                    if (parentOfTry != null) {
                                        //有catch就在finally给他关了(前提存在finally)，没有就建try/catch关掉
                                        //try放在最后判断好了，因为有些初始化在try里边儿 懂？？？
                                        final PsiCodeBlock finallyBlock = parentOfTry.getFinallyBlock();
                                        if (finallyBlock != null) {
                                            final PsiStatement[] statements = finallyBlock.getStatements();

                                            boolean isClose = false;
                                            for (PsiStatement statement : statements) {
                                                if (MoExpressionUtils.hasFullQualifiedName((PsiMethodCallExpression) ((PsiExpressionStatement) statement).getExpression(), "com.cw.wizbank.util.cwSQL", "cleanUp")) {
                                                    //LogUtils.debug("+================" + variable.getName());
                                                    final PsiExpression expressionFromText = factory.createExpressionFromText(variable.getName(), null);
                                                    ((PsiMethodCallExpression) ((PsiExpressionStatement) statement).getExpression()).getArgumentList().getExpressions()[0].replace(expressionFromText);
                                                    closeCount[1]++;
                                                    isClose = true;
                                                }
                                            }
                                            //没在finally关掉
                                            if (!isClose) {
                                                finallyBlock.addAfter(factory.createStatementFromText("cwSQL.cleanUp(" + variable.getName() + ",null);", null), finallyBlock.getFirstChild());
                                                closeCount[1]++;
                                            }

                                            //
                                        } else {
                                            //TODO 没有finally就加个

                                            PsiKeyword keyword = factory.createKeyword(PsiKeyword.FINALLY);
                                            PsiCodeBlock codeBlock = factory.createCodeBlockFromText("{cwSQL.cleanUp(" + variable.getName() + ",null);}", null);

                                            parentOfTry.getParent().addAfter(codeBlock, parentOfTry);
                                            parentOfTry.getParent().addAfter(keyword, parentOfTry);
                                            closeCount[1]++;

                                        }
                                    } else {
                                        // 不是在if/while里关，没有try, 否则在方法最后加入close
                                        final PsiMethod parentOfMethod = MoExpressionUtils.getParentOfMethod(finalLastRef.getElement());

                                        final PsiElement[] childs = parentOfMethod.getBody().getChildren();
                                        final PsiStatement statementFromText = factory.createStatementFromText(finalLastRef.getCanonicalText() + ".close();", null);
                                        for (int i1 = 0; i1 < childs.length; i1++) {
                                            if (childs[i1] instanceof PsiReturnStatement || i1 == childs.length - 1) {
                                                childs[i1].getParent().addBefore(statementFromText, childs[i1]);
                                                closeCount[1]++;
                                                break;
                                            }
                                        }
                                    }
                                }


                            }
                        });


                    }
                }
            }
        });
        String format = String.format("%s finished!not close stmt %d,rs %d;bot close stmt %d,rs %d!", psiFile.getName(), notCloseCount[0], notCloseCount[1], closeCount[0], closeCount[1]);
//        NotifyUtils.info(format, project);
        LogUtils.info(format);
        if (notCloseCount[0] != closeCount[0] || notCloseCount[1] != closeCount[1]) {
            LogUtils.info("please self check " + psiFile.getName());
        }
       /* psiFile.accept(new JavaRecursiveElementVisitor() {
            @Override public void visitReferenceExpression(PsiReferenceExpression ref) {
              //  //LogUtils.debug(ref.getQualifiedName());
                if ("stmt.executeUpdate".equals(ref.getQualifiedName())) {
                    //LogUtils.debug("yiny pstm:"+ref);
                    List<PsiReference> refPoints = MoExpressionUtils.getReferenceOnMethodScope(ref, ref.getTextOffset()-1);
                    for (PsiReference refPoint : refPoints) {
                        //LogUtils.debug(refPoint);
                    }
                }
                if ("rs.next".equals(ref.getQualifiedName())) {
                    //LogUtils.debug("yiny ResultSet:"+ref);
                    List<PsiReference> refPoints = MoExpressionUtils.getReferenceOnMethodScope(ref, ref.getTextOffset()-1);
                    for (PsiReference refPoint : refPoints) {
                        //LogUtils.debug(refPoint);
                    }
                }

            }
        });*/
    }

    public static boolean checkBranch(PsiStatement b) {
        final PsiElement[] children = b.getFirstChild().getChildren();
        boolean flag = false;
        for (PsiElement child : children) {
            if (((PsiExpressionStatement) child).getExpression() instanceof PsiReturnStatement) {
                flag = true;
            } else if (((PsiExpressionStatement) child).getExpression() instanceof PsiThrowStatement) {
                flag = true;
            }
        }
        return flag;
    }

    public static void fixBranch(PsiElementFactory factory, PsiStatement branch, PsiReference finalLastRef) {
        if (branch == null) {
            return;
        }
        final PsiElement[] children = branch.getFirstChild().getChildren();
        PsiElement lastexp = branch.getFirstChild().getFirstChild();

        for (PsiElement child : children) {
            if (child instanceof PsiExpressionStatement) {
                if (((PsiExpressionStatement) child).getExpression() instanceof PsiMethodCallExpression) {
                    PsiMethodCallExpression m = (PsiMethodCallExpression) ((PsiExpressionStatement) child).getExpression();
                    final PsiReferenceExpression r = m.getMethodExpression();
                    final PsiReference reference = r.getReference();
                    //LogUtils.debug("-------call-----" + reference);
                    lastexp = child;
                } else if (((PsiExpressionStatement) child).getExpression() instanceof PsiAssignmentExpression) {
                    PsiAssignmentExpression m = (PsiAssignmentExpression) ((PsiExpressionStatement) child).getExpression();
                    if (m.getRExpression() instanceof PsiMethodCallExpression) {
                        PsiMethodCallExpression g = (PsiMethodCallExpression) m.getRExpression();
                        PsiReferenceExpression rr = g.getMethodExpression();
                        PsiExpression qualifierExpression = rr.getQualifierExpression();
                        if (qualifierExpression != null && qualifierExpression.getReference() == finalLastRef) {
                            //LogUtils.debug("------ref----" + rr + "+--" + (qualifierExpression.getReference() == finalLastRef));
                            lastexp = child;
                        }

                    }
                } else {
                    //  lastexp = child;
                }
            }
        }
        //if里没return且else里没有throw，只能在最后那关 ，不能在if里关，否则会重复close npe
        //if里 return或else里 有throw 就两边都close

        lastexp.getParent().addAfter(factory.createStatementFromText(finalLastRef.getCanonicalText() + ".close();", null), lastexp);
    }

}
