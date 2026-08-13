package com.smart.phone.accessor;

import com.lowdragmc.lowdraglib2.syncdata.AccessorRegistries;
import com.lowdragmc.lowdraglib2.syncdata.accessor.direct.CustomDirectAccessor;
import com.mojang.serialization.Codec;
import com.smart.phone.network.c2s.ChatRoomImagePayload;
import com.smart.phone.ui.app.IApp;
import com.smart.phone.ui.data.IPhoneInfoData;
import com.smart.phone.ui.data.OfficialMessage;
import com.smart.phone.ui.data.PhoneInfo;
import com.smart.phone.ui.data.chat.ChatRoom;
import com.smart.phone.ui.data.chat.ChatRoomListSnapshot;
import com.smart.phone.ui.data.chat.ChatRoomMessage;
import com.smart.phone.ui.data.chat.ChatRoomSnapshot;
import com.smart.phone.ui.data.chat.ChatRoomSummary;
import com.smart.phone.ui.data.social.FriendEntry;
import com.smart.phone.ui.data.social.FriendListSnapshot;
import com.smart.phone.ui.data.social.FriendRecord;
import com.smart.phone.ui.time.IPhoneTimeSource;
import com.viscript_lib.annotation.ViScriptRegisterAccessors;
import com.viscript_lib.event.RegisterAccessorEvent;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * 手机模组需要在 LDLib2 RPC 扫描前注册的持久化访问器。
 */
public final class SmartPhoneAccessors {
    private SmartPhoneAccessors() {
    }

    @ViScriptRegisterAccessors
    public static void register(RegisterAccessorEvent event) {
        event.register(PhoneInfo.class, PhoneInfo::new);
        event.register(OfficialMessage.class, OfficialMessage::new);
        event.register(ChatRoomMessage.class, ChatRoomMessage::new);
        event.register(ChatRoom.class, ChatRoom::new);
        event.register(ChatRoomSummary.class, ChatRoomSummary::new);
        event.register(ChatRoomListSnapshot.class, ChatRoomListSnapshot::new);
        event.register(ChatRoomSnapshot.class, ChatRoomSnapshot::new);
        event.register(ChatRoomImagePayload.class, ChatRoomImagePayload::new);
        event.register(FriendRecord.class, FriendRecord::new);
        event.register(FriendEntry.class, FriendEntry::new);
        event.register(FriendListSnapshot.class, FriendListSnapshot::new);

        registerDispatchAccessor(IPhoneTimeSource.class, IPhoneTimeSource.CODEC, IPhoneTimeSource.STREAM_CODEC);
        registerDispatchAccessor(IApp.class, IApp.CODEC, IApp.STREAM_CODEC);
        registerDispatchAccessor(IPhoneInfoData.class, IPhoneInfoData.CODEC, IPhoneInfoData.STREAM_CODEC);
    }

    /**
     * 注册基于注册表派生的多态接口访问器，普通数据类直接用 {@link RegisterAccessorEvent#register}。
     */
    private static <T> void registerDispatchAccessor(Class<T> type, Codec<T> codec, StreamCodec<ByteBuf, T> streamCodec) {
        AccessorRegistries.registerAccessor(CustomDirectAccessor.builder(type)
                .codec(codec)
                .streamCodec(streamCodec)
                .codecMark()
                .build(), 0);
    }
}
