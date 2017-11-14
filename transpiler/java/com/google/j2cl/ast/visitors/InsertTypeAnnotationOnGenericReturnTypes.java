/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.j2cl.ast.visitors;

import com.google.j2cl.ast.AbstractRewriter;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.DeclaredTypeDescriptor;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.JsDocAnnotatedExpression;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.Node;
import com.google.j2cl.ast.TypeDescriptor;

/**
 * Inserts a cast for the return type of methods which return generic types. This avoids a potential
 * error in JSCompiler where the combination of type tightening and invariant generics can lead to
 * mismatched types.
 *
 * <p>This should not be necessary when using new type inference in JSCompiler.
 */
public class InsertTypeAnnotationOnGenericReturnTypes extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(new Rewriter());
  }

  private static class Rewriter extends AbstractRewriter {
    @Override
    public boolean shouldProcessJsDocAnnotatedExpression(JsDocAnnotatedExpression annotation) {
      if (annotation.getExpression() instanceof MethodCall) {
        MethodCall methodCall = (MethodCall) annotation.getExpression();
        for (Expression expression : methodCall.getArguments()) {
          // process arguments
          expression.accept(this);
        }
        // Don't rewrite the castExpression nor its sub expressions.
        return false;
      }
      return true;
    }

    @Override
    public Node rewriteMethodCall(MethodCall methodCall) {
      MethodDescriptor methodDeclaration = methodCall.getTarget().getDeclarationDescriptor();
      // Type variable should be declared in method to trigger inference.
      boolean methodDeclaresTypeVariables =
          !methodDeclaration.getTypeParameterTypeDescriptors().isEmpty();
      TypeDescriptor returnTypeDescriptor = methodDeclaration.getReturnTypeDescriptor();
      // Type variable should be used in method return type and return type should a generic type
      // for inference to matter (as mismatches becomes an issue due invariant generic type
      // parameters in OTI).
      boolean methodReturnHasTypeVariables = !returnTypeDescriptor.getAllTypeVariables().isEmpty();
      boolean methodReturnIsGenericType =
          returnTypeDescriptor instanceof DeclaredTypeDescriptor
              && ((DeclaredTypeDescriptor) returnTypeDescriptor).hasTypeArguments();

      // In reality, for an inference mismatch to occur, type variable used in return should be
      // declared by the method. However there is no easy way to check that right now so we are
      // approximating here since extra casts does only hurt uncompiled size.
      if (methodDeclaresTypeVariables
          && methodReturnHasTypeVariables
          && methodReturnIsGenericType) {
        return JsDocAnnotatedExpression.newBuilder()
            .setExpression(methodCall)
            .setAnnotationType(methodCall.getTypeDescriptor())
            .build();
      }
      return methodCall;
    }
  }
}
