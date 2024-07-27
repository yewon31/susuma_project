package com.susuma.point.model;

import java.io.IOException;
import java.util.List;

import com.susuma.member.model.MemberDTO;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface PointMapper {
	
	Integer MemberPoints(String meNo);
    List<PointDTO> getPointEarnings(String meNo);
    List<PointDTO> getPointSpendings(String meNo);
    void updateMemberPoints(String meNo, Integer point);
    void addSpendingHistory(String meNo, Integer point);
    void addEarningHistory(String meNo,  Integer point);

}
