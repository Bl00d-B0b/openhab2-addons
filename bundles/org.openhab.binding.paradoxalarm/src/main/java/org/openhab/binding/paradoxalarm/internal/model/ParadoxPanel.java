/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.paradoxalarm.internal.model;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.binding.paradoxalarm.internal.communication.IDataUpdateListener;
import org.openhab.binding.paradoxalarm.internal.communication.IParadoxCommunicator;
import org.openhab.binding.paradoxalarm.internal.exceptions.ParadoxRuntimeException;
import org.openhab.binding.paradoxalarm.internal.parsers.EvoParser;
import org.openhab.binding.paradoxalarm.internal.parsers.IParadoxParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ParadoxPanel} Composition class which contains all Paradox entities.
 *
 * @author Konstantin Polihronov - Initial contribution
 */
public class ParadoxPanel implements IDataUpdateListener {

    private final Logger logger = LoggerFactory.getLogger(ParadoxPanel.class);

    @NonNull
    private static ParadoxPanel paradoxPanel = new ParadoxPanel();

    private ParadoxInformation panelInformation;
    private List<Partition> partitions;
    private List<Zone> zones;
    private IParadoxParser parser;
    private IParadoxCommunicator communicator;
    private double acLevel;
    private double batteryLevel;
    private double dcLevel;
    private ZonedDateTime panelTime;

    private ParadoxPanel() {
        this.parser = new EvoParser();
    }

    public void createModelEntities() {
        byte[] panelInfoBytes = communicator.getPanelInfoBytes();
        panelInformation = new ParadoxInformation(panelInfoBytes, parser);

        if (isPanelSupported()) {
            logger.info("Paradox system is supported. Panel data retrieved={} ", panelInformation);
            createPartitions();
            createZones();
        } else {
            throw new ParadoxRuntimeException(
                    "Unsupported panel type. Type: " + panelInformation.getPanelType().name());
        }
    }

    public static ParadoxPanel getInstance() {
        return paradoxPanel;
    }

    public boolean isPanelSupported() {
        PanelType panelType = panelInformation.getPanelType();
        return panelType == PanelType.EVO48 || panelType == PanelType.EVO192 || panelType == PanelType.EVOHD;
    }

    public void updateEntitiesStates() {
        if (!isOnline()) {
            logger.debug("Not online. Unable to update entities states. ");
            return;
        }

        List<byte[]> currentPartitionFlags = communicator.getPartitionFlags();
        for (int i = 0; i < partitions.size(); i++) {
            Partition partition = partitions.get(i);
            if (i < currentPartitionFlags.size()) {
                partition.setState(parser.calculatePartitionState(currentPartitionFlags.get(i)));
            } else {
                logger.debug("Partition flags collection is smaller than the number of partitions.");
            }
        }

        ZoneStateFlags zoneStateFlags = communicator.getZoneStateFlags();
        for (int i = 0; i < zones.size(); i++) {
            Zone zone = zones.get(i);
            zone.setZoneState(parser.calculateZoneState(zone.getId(), zoneStateFlags));
        }

        byte[] firstPage = communicator.getMemoryMap().getElement(0);
        int year = firstPage[18] * 100 + firstPage[19];
        panelTime = ZonedDateTime.of(
                LocalDateTime.of(year, firstPage[20], firstPage[21], firstPage[22], firstPage[23], firstPage[24]),
                TimeZone.getDefault().toZoneId());
        acLevel = firstPage[25] & 0xFF;
        acLevel = Math.max(acLevel * (20.3 - 1.4) / 255.0 + 1.4, 0);
        dcLevel = firstPage[27] & 0xFF;
        dcLevel = Math.max(0, dcLevel * 22.8 / 255);
        batteryLevel = firstPage[26] & 0xFF;
        batteryLevel = Math.max(0, batteryLevel * 22.8 / 255);
    }

    private List<Zone> createZones() {
        zones = new ArrayList<>();
        Map<Integer, String> zoneLabels = communicator.getZoneLabels();
        for (int i = 0; i < zoneLabels.size(); i++) {
            Zone zone = new Zone(i + 1, zoneLabels.get(i));
            zones.add(zone);
        }
        return zones;
    }

    private List<Partition> createPartitions() {
        partitions = new ArrayList<>();
        Map<Integer, String> partitionLabels = communicator.getPartitionLabels();
        for (int i = 0; i < partitionLabels.size(); i++) {
            Partition partition = new Partition(i + 1, partitionLabels.get(i));
            partitions.add(partition);
            logger.debug("Partition {}:\t{}", i + 1, partition.getState().getMainState());
        }
        return partitions;
    }

    @Override
    public void update() {
        if (panelInformation == null || partitions == null || zones == null) {
            createModelEntities();
        }
        updateEntitiesStates();
    }

    public void dispose() {
        this.panelInformation = null;
        this.partitions = null;
        this.zones = null;
    }

    public ParadoxInformation getPanelInformation() {
        return panelInformation;
    }

    public List<Partition> getPartitions() {
        return partitions;
    }

    public void setPartitions(List<Partition> partitions) {
        this.partitions = partitions;
    }

    public List<Zone> getZones() {
        return zones;
    }

    public void setZones(List<Zone> zones) {
        this.zones = zones;
    }

    public boolean isOnline() {
        return communicator != null && communicator.isOnline();
    }

    public double getVdcLevel() {
        return acLevel;
    }

    public double getBatteryLevel() {
        return batteryLevel;
    }

    public double getDcLevel() {
        return dcLevel;
    }

    public ZonedDateTime getPanelTime() {
        return panelTime;
    }

    public void setCommunicator(IParadoxCommunicator communicator) {
        this.communicator = communicator;
    }

    public IParadoxCommunicator getCommunicator() {
        return communicator;
    }

    @Override
    public String toString() {
        return "ParadoxPanel [panelInformation=" + panelInformation + "]";
    }
}
