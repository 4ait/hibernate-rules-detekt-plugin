package ru.code4a.detekt.plugin.hibernate.extentions.psi

import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtThisExpression
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.util.getResolvedCall

fun PsiElement.determineClassSelfMutateByPsiElement(bindingContext: BindingContext): PsiElement? {
  var mutatePsiElement: PsiElement? = null

  this.accept(
    object : KtTreeVisitorVoid() {
      override fun visitBinaryExpression(expression: KtBinaryExpression) {
        super.visitBinaryExpression(expression)
        if (expression.operationToken in mutableListOf(KtTokens.EQ, KtTokens.PLUSEQ, KtTokens.MINUSEQ, KtTokens.MULTEQ, KtTokens.DIVEQ)) {
          val left = expression.left

          if (left is KtDotQualifiedExpression) {
            val isSkip =
              (left.selectorExpression as? KtNameReferenceExpression)
                ?.hasAnyAnnotation(listOf("jakarta.persistence.Transient"), bindingContext) ?: false // TODO(configuration)

            if (isSkip) {
              return
            }

            if (left.receiverExpression is KtThisExpression || left.isClassMember(bindingContext)) {
              mutatePsiElement = expression.psiOrParent
            }
          } else if (left is KtNameReferenceExpression && left.isClassMember(bindingContext)) {
            val isSkip = left.hasAnyAnnotation(listOf("jakarta.persistence.Transient"), bindingContext) // TODO(configuration)

            if (isSkip) {
              return
            }

            mutatePsiElement = expression.psiOrParent
          }
        }
      }

      override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)
        val calleeExpression = expression.calleeExpression
        if (calleeExpression is KtNameReferenceExpression) {
          val resolvedCall = calleeExpression.getResolvedCall(bindingContext)
          val resultingDescriptor = resolvedCall?.resultingDescriptor
          if (resultingDescriptor != null && resultingDescriptor.isCollectionMutation()) {
            val receiver = resolvedCall.dispatchReceiver
            if (receiver != null &&
              (
                receiver.type.constructor.declarationDescriptor
                  ?.isClassOwner() == true ||
                  receiver.isClassMember(bindingContext)
              )
            ) {
              mutatePsiElement = expression.psiOrParent
            }
          }
        }
      }
    }
  )

  return mutatePsiElement
}
