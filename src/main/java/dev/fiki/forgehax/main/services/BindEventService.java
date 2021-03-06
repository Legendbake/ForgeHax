package dev.fiki.forgehax.main.services;

import dev.fiki.forgehax.api.cmd.settings.KeyBindingSetting;
import dev.fiki.forgehax.api.event.SubscribeListener;
import dev.fiki.forgehax.api.events.game.KeyInputEvent;
import dev.fiki.forgehax.api.events.game.MouseInputEvent;
import dev.fiki.forgehax.api.events.render.GuiChangedEvent;
import dev.fiki.forgehax.api.key.BindingHelper;
import dev.fiki.forgehax.api.key.KeyBindingEx;
import dev.fiki.forgehax.api.mod.ServiceMod;
import dev.fiki.forgehax.api.modloader.RegisterMod;
import dev.fiki.forgehax.asm.events.packet.PacketOutboundEvent;
import net.minecraft.client.gui.screen.MainMenuScreen;
import net.minecraft.client.util.InputMappings;
import net.minecraft.network.play.client.CClientSettingsPacket;
import org.lwjgl.glfw.GLFW;

import static dev.fiki.forgehax.api.cmd.settings.KeyBindingSetting.*;

@RegisterMod
public class BindEventService extends ServiceMod {
  private boolean bindConfigLoaded = false;

  private void updateBindings(KeyBindingSetting setting, int keyAction) {
    final KeyBindingEx key = setting.getKeyBinding();
    switch (keyAction) {
      case GLFW.GLFW_PRESS:
        key.setPressed(true);
        setting.getListeners(IKeyPressedListener.class)
            .forEach(l -> l.onKeyPressed(key));
        break;
      case GLFW.GLFW_REPEAT:
        key.setPressed(true);
        setting.getListeners(IKeyDownListener.class)
            .forEach(l -> l.onKeyDown(key));
        break;
      case GLFW.GLFW_RELEASE:
        key.setPressed(false);
        setting.getListeners(IKeyReleasedListener.class)
            .forEach(l -> l.onKeyReleased(key));
        break;
    }
  }

  @SubscribeListener
  public void onKeyboardEvent(KeyInputEvent event) {
    for (KeyBindingSetting setting : getRegistry()) {
      if (InputMappings.Type.KEYSYM.equals(setting.getKeyInput().getType())
          && setting.getKeyBinding().matchesKey(event.getKey(), event.getScanCode())
          && setting.getKeyBinding().checkConflicts()) {
        updateBindings(setting, event.getAction());
      }
    }
  }

  @SubscribeListener
  public void onMouseEvent(MouseInputEvent event) {
    for (KeyBindingSetting setting : getRegistry()) {
      if (InputMappings.Type.MOUSE.equals(setting.getKeyInput().getType())
          && setting.getKeyCode() == event.getButton()
          && setting.getKeyBinding().checkConflicts()) {
        updateBindings(setting, event.getAction());
      }
    }
  }

  @SubscribeListener
  public void onGuiOpened(GuiChangedEvent event) {
    if (!bindConfigLoaded && event.getGui() instanceof MainMenuScreen) {
      bindConfigLoaded = true;
      // TODO: load config
    }
  }

  @SubscribeListener
  public void onPacketOutgoing(PacketOutboundEvent event) {
    if (BindingHelper.isSuppressingSettingsPacket()
        && event.getPacket() instanceof CClientSettingsPacket) {
      event.setCanceled(false);
    }
  }
}
