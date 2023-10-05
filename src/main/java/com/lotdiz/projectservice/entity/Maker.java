package com.lotdiz.projectservice.entity;

import com.lotdiz.projectservice.entity.common.BaseEntity;
import lombok.*;
import org.hibernate.annotations.Check;

import javax.persistence.*;

@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@Getter
@Entity
@Table(name = "maker")
public class Maker extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "maker_id")
    private Long makerId;

    @Column(name = "member_id", unique = true, nullable = false)
    private Long memberId;

    @Column(name = "maker_name", nullable = false)
    private String makerName;

    @Column(name = "maker_email", nullable = false)
    @Check(constraints = "maker_email LIKE '%@%'")
    private String makerEmail;

    @Column(name = "maker_phone_number", nullable = false)
    private String makerPhoneNumber;

    @Column(name = "maker_kakao_url")
    private String makerKakaoUrl;

    @Column(name = "maker_home_url")
    private String makerHomeUrl;

    @Column(name = "maker_sns_url")
    private String makerSnsUrl;

}
