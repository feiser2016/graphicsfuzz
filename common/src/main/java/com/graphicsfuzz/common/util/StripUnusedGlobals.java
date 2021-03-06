/*
 * Copyright 2018 The GraphicsFuzz Project Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.graphicsfuzz.common.util;

import com.graphicsfuzz.common.ast.TranslationUnit;
import com.graphicsfuzz.common.ast.decl.Declaration;
import com.graphicsfuzz.common.ast.decl.VariableDeclInfo;
import com.graphicsfuzz.common.ast.decl.VariablesDeclaration;
import com.graphicsfuzz.common.ast.expr.VariableIdentifierExpr;
import com.graphicsfuzz.common.transformreduce.Constants;
import com.graphicsfuzz.common.typing.ScopeEntry;
import com.graphicsfuzz.common.typing.ScopeTreeBuilder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StripUnusedGlobals extends ScopeTreeBuilder {

  public static void strip(TranslationUnit tu) {
    new StripUnusedGlobals(tu);
  }

  private Set<VariableDeclInfo> unusedGlobals;

  private StripUnusedGlobals(TranslationUnit tu) {
    this.unusedGlobals = new HashSet<>();
    visit(tu);
    sweep(tu);
  }

  @Override
  public void visitVariableDeclInfo(VariableDeclInfo variableDeclInfo) {
    super.visitVariableDeclInfo(variableDeclInfo);
    if (atGlobalScope() && !variableDeclInfo.getName().equals(Constants.INJECTION_SWITCH)) {
      // Initially, assume it is unused
      unusedGlobals.add(variableDeclInfo);
    }
  }

  @Override
  public void visitVariableIdentifierExpr(VariableIdentifierExpr variableIdentifierExpr) {
    super.visitVariableIdentifierExpr(variableIdentifierExpr);
    final ScopeEntry scopeEntry = currentScope.lookupScopeEntry(variableIdentifierExpr.getName());
    if (scopeEntry != null && scopeEntry.hasVariableDeclInfo()) {
      // If this is a global, mark it as used.
      unusedGlobals.remove(scopeEntry.getVariableDeclInfo());
    }
  }

  private void sweep(TranslationUnit tu) {
    final List<Declaration> oldTopLevelDecls = new ArrayList<>();
    oldTopLevelDecls.addAll(tu.getTopLevelDeclarations());
    for (Declaration decl : oldTopLevelDecls) {
      if (!(decl instanceof VariablesDeclaration)) {
        continue;
      }
      final VariablesDeclaration variablesDeclaration = (VariablesDeclaration) decl;
      int index = 0;
      while (index < variablesDeclaration.getNumDecls()) {
        if (unusedGlobals.contains(variablesDeclaration.getDeclInfo(index))) {
          variablesDeclaration.removeDeclInfo(index);
        } else {
          index++;
        }
      }
      if (variablesDeclaration.getNumDecls() == 0) {
        tu.removeTopLevelDeclaration(variablesDeclaration);
      }
    }
  }

}
