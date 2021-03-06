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

package com.graphicsfuzz.generator.transformation.mutator;

import static org.junit.Assert.assertEquals;

import com.graphicsfuzz.common.ast.TranslationUnit;
import com.graphicsfuzz.common.glslversion.ShadingLanguageVersion;
import com.graphicsfuzz.common.typing.Typer;
import com.graphicsfuzz.common.util.CannedRandom;
import com.graphicsfuzz.common.util.Helper;
import com.graphicsfuzz.common.util.ShaderKind;
import com.graphicsfuzz.generator.util.GenerationParams;
import java.util.List;
import org.junit.Test;

public class MutationPointsTest {

  @Test
  public void testNumMutationPoints() throws Exception {
    final String program =
        "void main() {\n"
            + "  int x = 0;\n"
            + "  int j = 0;\n"
            + "  for (int x = 0; x < 100; x++) {\n"
            + "    j += x;\n"
            + "  }\n"
            + "}\n";
    final TranslationUnit tu = Helper.parse(program, false);
    final ShadingLanguageVersion shadingLanguageVersion = ShadingLanguageVersion.GLSL_440;
    final MutationPoints mutationPoints = new MutationPoints(
        new Typer(tu, shadingLanguageVersion),
        tu,
        shadingLanguageVersion,
        new CannedRandom(new Object[] { }),
        GenerationParams.normal(ShaderKind.FRAGMENT));
    final List<IMutationPoint> points = mutationPoints.getAllMutationPoints();
    // The mutation points are:
    // - LHS and RHS of "x < 100"
    // - RHS of "j += x"
    // All others are currently disabled, either due to having a const context, being l-values,
    // or not being the children of expressions.
    // In due course it would be good to me more general.
    assertEquals(3, points.size());
  }

  @Test
  public void testNumMutationPoints100WebGL() throws Exception {
    final String program =
        "void main() {\n"
            + "  int x = 0;\n"
            + "  int j = 0;\n"
            + "  for (int x = 0; x < 100; x++) {\n"
            + "    j += x;\n"
            + "  }\n"
            + "}\n";
    final TranslationUnit tu = Helper.parse(program, false);
    final ShadingLanguageVersion shadingLanguageVersion = ShadingLanguageVersion.WEBGL_SL;
    final MutationPoints mutationPoints = new MutationPoints(
        new Typer(tu, shadingLanguageVersion),
        tu,
        shadingLanguageVersion,
        new CannedRandom(new Object[] { }),
        GenerationParams.normal(ShaderKind.FRAGMENT));
    final List<IMutationPoint> points = mutationPoints.getAllMutationPoints();
    // Only a single mutation point: the loop guard is untouchable as this is GLSL 100.
    assertEquals(1, points.size());
  }

  @Test
  public void testNoMutationPoints100WebGL() throws Exception {
    final String program =
        "void main() {\n"
            + "  int j = 0;"
            + "  for (int x = 0; x < 100; x++) {\n"
            + "    j = j + 1;"
            + "  }\n"
            + "}\n";
    final TranslationUnit tu = Helper.parse(program, false);
    final ShadingLanguageVersion shadingLanguageVersion = ShadingLanguageVersion.WEBGL_SL;
    final MutationPoints mutationPoints = new MutationPoints(
        new Typer(tu, shadingLanguageVersion),
        tu,
        shadingLanguageVersion,
        new CannedRandom(new Object[] { }),
        GenerationParams.normal(ShaderKind.FRAGMENT));
    final List<IMutationPoint> points = mutationPoints.getAllMutationPoints();
    // Two mutation points: LHS and RHS of "j + 1", and RHS of "j = j + 1".
    // Loop guard is untouchable as this is GLSL 100.
    assertEquals(3, points.size());
  }

}