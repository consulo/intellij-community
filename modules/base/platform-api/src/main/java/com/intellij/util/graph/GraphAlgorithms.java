/*
 * Copyright 2000-2011 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.util.graph;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.util.Chunk;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author nik
 */
public abstract class GraphAlgorithms {
  public static GraphAlgorithms getInstance() {
    return ServiceManager.getService(GraphAlgorithms.class);
  }

  @Nullable
  public abstract <Node> List<Node> findShortestPath(@Nonnull Graph<Node> graph, @Nonnull Node start, @Nonnull Node finish);

  @Nonnull
  public abstract <Node> List<List<Node>> findKShortestPaths(@Nonnull Graph<Node> graph, @Nonnull Node start, @Nonnull Node finish, int k,
                                                             @Nonnull ProgressIndicator progressIndicator);

  @Nonnull
  public abstract <Node> Set<List<Node>> findCycles(@Nonnull Graph<Node> graph, @Nonnull Node node);

  @Nonnull
  public abstract <Node> List<List<Node>> removePathsWithCycles(@Nonnull List<List<Node>> paths);

  @Nonnull
  public abstract <Node> Graph<Node> invertEdgeDirections(@Nonnull Graph<Node> graph);

  @Nonnull
  public abstract <Node> Collection<Chunk<Node>> computeStronglyConnectedComponents(@Nonnull Graph<Node> graph);

  @Nonnull
  public abstract <Node> Graph<Chunk<Node>> computeSCCGraph(@Nonnull Graph<Node> graph);

  /**
   * Adds start node and all its outs to given set recursively.
   * Nodes which are already in set aren't processed.
   *
   * @param start node to start from
   * @param set set to be populated
   */
  public abstract <Node> void collectOutsRecursively(@Nonnull Graph<Node> graph, Node start, Set<Node> set);
}
