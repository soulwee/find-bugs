package com.gudong.action;

import com.gudong.utils.LogUtils;
import com.gudong.utils.NotifyUtils;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * description 检查资源
 *
 * @author maggie
 * @date 2021-08-27 15:42
 */
public class FindResourceAction extends AnAction {
    private static final AtomicBoolean IS_CHECK = new AtomicBoolean(false);

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        // 保证单次单个任务
        Project project = event.getProject();
        try {
            if (IS_CHECK.get()) {
                NotifyUtils.info("正在检查代码中，请稍后再试。。。", project);
                return;
            }
            IS_CHECK.set(true);
            VirtualFile file = CommonDataKeys.VIRTUAL_FILE.getData(event.getDataContext());
            final String path = file.getCanonicalPath();
            //LogUtils.debug(path);

            final Collection<VirtualFile> files = getAllSubFiles(file);
            FixCode.scan(project, files);

            // 检查单个文件
            /*final PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
            scanCode(project, notCloseCount, closeCount, psiFile);*/
       /* final PsiElement[] children = psiFile.getChildren();
        for (PsiElement child : children) {
            //LogUtils.debug(child);
        }*/

          /*  psiFile.accept(new JavaRecursiveElementVisitor() {
                @Override
                public void visitMethodCallExpression(PsiMethodCallExpression expression) {

                    if (MoExpressionUtils.hasFullQualifiedName(expression, "java.sql.Connection", "prepareStatement")) {
                        PsiMethod method = MoExpressionUtils.getParentOfMethod(expression);
                        if (method != null) {
                            //LogUtils.debug("parent " + method);
                        }
                        final PsiReference[] references = expression.getReferences();
                        for (PsiReference reference : references) {
                            //LogUtils.debug("ref:" + reference);
                        }
                        //LogUtils.debug(expression);
                    }
                    if (MoExpressionUtils.hasFullQualifiedName(expression, "java.sql.PreparedStatement", "executeQuery")) {
                        PsiMethod method = MoExpressionUtils.getParentOfMethod(expression);
                        if (method != null) {
                            //LogUtils.debug("parent " + method);
                        }
                        //LogUtils.debug(expression);
                    }
                }
            });*/
            IS_CHECK.set(false);
        } catch (Exception e) {
            NotifyUtils.error("检查出错。。。" + e.getMessage(), project);
            e.printStackTrace();
        } finally {
            NotifyUtils.info("check-bot fix finished!", project);
            IS_CHECK.set(false);
        }
    }

    public static Collection<VirtualFile> getAllSubFiles(VirtualFile virtualFile) {
        Collection<VirtualFile> list = new ArrayList<>();
        VfsUtilCore.visitChildrenRecursively(virtualFile, new VirtualFileVisitor() {
            @Override
            public boolean visitFile(@NotNull VirtualFile file) {
                if (!file.isDirectory()) {
                    list.add(file);
                }
                return super.visitFile(file);
            }
        });

        return list;
    }


}
