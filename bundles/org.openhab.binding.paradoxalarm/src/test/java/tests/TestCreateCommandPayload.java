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
package tests;

import org.junit.Assert;
import org.junit.Test;
import org.openhab.binding.paradoxalarm.internal.communication.messages.CommandPayload;
import org.openhab.binding.paradoxalarm.internal.communication.messages.PartitionCommand;
import org.openhab.binding.paradoxalarm.internal.util.ParadoxUtil;

/**
 * The {@link TestCreateCommandPayload} This test tests the proper build of command payload object for partition entity
 * with all types of commands.
 *
 * @author Konstantin Polihronov - Initial contribution
 */
public class TestCreateCommandPayload {

    static {
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "TRACE");
    }

    @Test
    public void testCreatePayload() {
        for (PartitionCommand command : PartitionCommand.values()) {
            for (int partitionNumber = 1; partitionNumber <= 8; partitionNumber++) {
                CommandPayload payload = new CommandPayload(partitionNumber, command);
                assertNibble(partitionNumber, command, payload);
            }
        }
    }

    private void assertNibble(int partitionNumber, PartitionCommand command, CommandPayload payload) {
        byte[] bytes = payload.getBytes();
        int payloadIndexOfByteToCheck = 6 + (partitionNumber - 1) / 2;
        byte byteValue = bytes[payloadIndexOfByteToCheck];
        if ((partitionNumber - 1) % 2 == 0) {
            Assert.assertTrue(ParadoxUtil.getHighNibble(byteValue) == command.getCommand());
        } else {
            Assert.assertTrue(ParadoxUtil.getLowNibble(byteValue) == command.getCommand());
        }
    }

}
