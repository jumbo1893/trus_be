package com.jumbo.trus.dto.auth;

import com.jumbo.trus.dto.player.PlayerDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserSetup {

    private UserDTO currentUser;

    private List<PlayerDTO> eligiblePlayersToPairWith;

    private List<UserDTO> eligibleUsersToSendNotification;

    private PlayerDTO primaryPlayer;

}
