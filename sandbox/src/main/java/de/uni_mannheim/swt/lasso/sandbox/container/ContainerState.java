/*
 * LASSO - an Observatorium for the Dynamic Selection, Analysis and Comparison of Software
 * Copyright (C) 2024 Marcus Kessel (University of Mannheim) and LASSO contributers
 *
 * This file is part of LASSO.
 *
 * LASSO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LASSO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LASSO.  If not, see <https://www.gnu.org/licenses/>.
 */
package de.uni_mannheim.swt.lasso.sandbox.container;

import com.github.dockerjava.api.command.InspectContainerResponse;

/**
 *
 * @author Marcus Kessel
 */
public class ContainerState {

    private Container container;
    private String containerId;

    public ContainerState(Container container, String containerId) {
        this.container = container;
        this.containerId = containerId;
    }

    public boolean isRunning() {
        Boolean running = getInspectContainerResponse().getState().getRunning();

        return running != null && running;
    }

    public InspectContainerResponse getInspectContainerResponse() {
        return container.getContainerService()
                .getContainerStatus(containerId);
    }

    public Container getContainer() {
        return container;
    }

    public void setContainer(Container container) {
        this.container = container;
    }

    public String getContainerId() {
        return containerId;
    }

    public void setContainerId(String containerId) {
        this.containerId = containerId;
    }
}
