package com.winlator.xserver

import com.winlator.xconnector.XOutputStream
import com.winlator.xconnector.XStreamLock
import com.winlator.xserver.events.Event
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class InputDeviceManagerPointerGrabTest {
    @Test
    fun `explicit grab delivers button press selected by grab mask`() {
        val server = XServer(ScreenInfo(800, 600), true)
        val output = grabbedOutput(server, Event.BUTTON_PRESS)

        server.injectPointerButtonPress(Pointer.Button.BUTTON_LEFT)

        assertEquals(4, output.buffer.get(0).toInt())
        assertEquals(0, output.buffer.getShort(28).toInt())
    }

    @Test
    fun `explicit grab delivers button release selected by grab mask`() {
        val server = XServer(ScreenInfo(800, 600), true)
        val output = grabbedOutput(server, Event.BUTTON_RELEASE)

        server.injectPointerButtonPress(Pointer.Button.BUTTON_LEFT)
        server.injectPointerButtonRelease(Pointer.Button.BUTTON_LEFT)

        assertEquals(5, output.buffer.get(0).toInt())
        assertEquals(Pointer.Button.BUTTON_LEFT.flag(), output.buffer.getShort(28).toInt())
    }

    private fun grabbedOutput(server: XServer, mask: Int): XOutputStream {
        val client = mock(XClient::class.java)
        val output = object : XOutputStream(64) {
            override fun lock(): XStreamLock = XStreamLock { }
        }
        `when`(client.outputStream).thenReturn(output)
        server.grabManager.activatePointerGrab(
            server.windowManager.rootWindow, false, Bitmask(mask.toLong()), client, null
        )
        return output
    }
}
