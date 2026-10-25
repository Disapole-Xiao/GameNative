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
        val (pointer, grabManager, rootWindow) = pointerFixture()
        val output = grabbedOutput(grabManager, rootWindow, Event.BUTTON_PRESS)

        pointer.setButton(Pointer.Button.BUTTON_LEFT, true)

        assertEquals(4, output.buffer.get(0).toInt())
        assertEquals(0, output.buffer.getShort(28).toInt())
    }

    @Test
    fun `explicit grab delivers button release selected by grab mask`() {
        val (pointer, grabManager, rootWindow) = pointerFixture()
        val output = grabbedOutput(grabManager, rootWindow, Event.BUTTON_RELEASE)

        pointer.setButton(Pointer.Button.BUTTON_LEFT, true)
        pointer.setButton(Pointer.Button.BUTTON_LEFT, false)

        assertEquals(5, output.buffer.get(0).toInt())
        assertEquals(Pointer.Button.BUTTON_LEFT.flag(), output.buffer.getShort(28).toInt())
    }

    private fun pointerFixture(): Triple<Pointer, GrabManager, Window> {
        val server = mock(XServer::class.java)
        val rootWindow = Window(1, null, 0, 0, 800, 600, null)
        val windowManager = mock(WindowManager::class.java)
        setField(WindowManager::class.java, windowManager, "rootWindow", rootWindow)
        setField(XServer::class.java, server, "windowManager", windowManager)

        val pointer = Pointer(server)
        setField(XServer::class.java, server, "pointer", pointer)
        val keyboard = mock(Keyboard::class.java)
        `when`(keyboard.modifiersMask).thenReturn(Bitmask())
        setField(XServer::class.java, server, "keyboard", keyboard)

        val inputDeviceManager = InputDeviceManager(server)
        setField(XServer::class.java, server, "inputDeviceManager", inputDeviceManager)
        val grabManager = GrabManager(server)
        setField(XServer::class.java, server, "grabManager", grabManager)
        return Triple(pointer, grabManager, rootWindow)
    }

    private fun grabbedOutput(grabManager: GrabManager, rootWindow: Window, mask: Int): XOutputStream {
        val client = mock(XClient::class.java)
        val output = object : XOutputStream(64) {
            override fun lock(): XStreamLock = XStreamLock { }
        }
        `when`(client.outputStream).thenReturn(output)
        grabManager.activatePointerGrab(
            rootWindow, false, Bitmask(mask.toLong()), client, null
        )
        return output
    }

    private fun setField(owner: Class<*>, instance: Any, name: String, value: Any) {
        owner.getDeclaredField(name).apply { isAccessible = true }.set(instance, value)
    }
}
