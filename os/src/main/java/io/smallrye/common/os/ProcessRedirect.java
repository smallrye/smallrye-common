/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.smallrye.common.os;

/**
 * Extra process redirections.
 *
 * @deprecated Use {@link ProcessBuilder.Redirect#DISCARD} instead.
 */
@Deprecated(since = "2.4", forRemoval = true)
public final class ProcessRedirect {
    private ProcessRedirect() {
    }

    /**
     * {@return the discarding process redirection}
     */
    public static ProcessBuilder.Redirect discard() {
        return ProcessBuilder.Redirect.DISCARD;
    }
}
