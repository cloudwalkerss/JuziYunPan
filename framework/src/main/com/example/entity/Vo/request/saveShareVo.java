package com.example.entity.Vo.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class saveShareVo {
    String shareId;
    String shareFileIds;
    String myFolderId;
    String code;

}
