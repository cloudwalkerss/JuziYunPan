package com.example.entity.Vo.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@Data
@NoArgsConstructor
public class checkCodeVo {
  private String code;
  private String shareId;
}
