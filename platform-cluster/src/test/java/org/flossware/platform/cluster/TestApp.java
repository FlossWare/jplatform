/*
 * Copyright (C) 2024-2026 FlossWare
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.flossware.platform.cluster;

import org.flossware.platform.api.Application;
import org.flossware.platform.api.ApplicationContext;
import org.flossware.platform.api.ApplicationShutdownException;
import org.flossware.platform.api.ApplicationStartupException;

/**
 * Simple test application for unit tests.
 * Implements both Application interface and provides a main() method
 * for testing purposes.
 */
public class TestApp implements Application {

    @Override
    public void start(ApplicationContext context) throws ApplicationStartupException {
        // Do nothing - just for testing
    }

    @Override
    public void stop() throws ApplicationShutdownException {
        // Do nothing - just for testing
    }

    /**
     * Main method for testing non-Application deployments.
     */
    public static void main(String[] args) {
        // Do nothing - just for testing
    }
}
