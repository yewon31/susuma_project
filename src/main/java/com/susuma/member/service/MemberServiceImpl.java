package com.susuma.member.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import com.susuma.category.model.CategoryDTO;
import com.susuma.category.model.CategoryMapper;
import com.susuma.member.model.MemberDTO;
import com.susuma.member.model.MemberMapper;

import com.susuma.util.mybatis.MybatisUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;

public class MemberServiceImpl implements MemberService {

	private SqlSessionFactory sqlSessionFactory = MybatisUtil.getSqlSessionFactory();
	public final int recordsPerPage = 10; // 한 페이지당 보여줄 레코드 수

	/**
	 * 파일 데이터를 수신해서 바이트 배열 형태로 반환(경로 지정 x 파일 시스템에 저장 x) 추후 데이터베이스의 BLOB (Binary
	 * Large Object) 필드에 저장 '@MultipartConfig()' 어노테이션은 MemberController.java
	 */
	private byte[] getFileBytes(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

		Part filePart = request.getPart("profilePhoto");

		if (filePart != null && filePart.getSize() > 0) { // 파일이 존재하는 경우

			InputStream inputStream = filePart.getInputStream(); // 파일의 입력 스트림을 얻어 파일 데이터를 읽음
			byte[] fileBytes = new byte[(int) filePart.getSize()]; // 파일 크기만큼의 바이트 배열을 생성
			inputStream.read(fileBytes); // 입력 스트림에서 파일 데이터를 읽어 바이트 배열에 저장
			return fileBytes; // 파일 데이터가 담긴 배열 반환

		} else {
			System.out.println("No file uploaded."); // 파일이 업로드되지 않은 경우 콘솔에 메시지 출력
		}

		return null; // 파일이 업로드되지 않은 경우 빈 문자열 반환
	}

	/**
	 * DTO 생성
	 */
	private MemberDTO createMemberDTO(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

		String meNo = request.getParameter("meNo");
		String type = request.getParameter("type");
		String email = request.getParameter("email");
		String pw = request.getParameter("pw");
		String name = request.getParameter("name");
		String phoneNum = request.getParameter("phoneNum");
		String address = request.getParameter("address");
		String addressDetail = request.getParameter("addressDetail");
		Double latitude = (request.getParameter("latitude") != null && !request.getParameter("latitude").isEmpty()) ? Double.parseDouble(request.getParameter("latitude")) : 0.0;
		Double longitude = (request.getParameter("longitude") != null && !request.getParameter("longitude").isEmpty()) ? Double.parseDouble(request.getParameter("longitude")) : 0.0;
		String emailNotification = request.getParameter("emailNotification");
		emailNotification = emailNotification == null ? "N" : emailNotification;
		String businessNumber = request.getParameter("businessNumber");
		String shortDescription = request.getParameter("shortDescription");
		String maxDistance = request.getParameter("maxDistance");
		String description = request.getParameter("description");
		String workHours = request.getParameter("workHours");
		String joinApproval = request.getParameter("joinApproval");
		joinApproval = joinApproval == null ? "N" : joinApproval;
		String caNo = request.getParameter("caNo");
		String point = request.getParameter("point");
		String status = request.getParameter("status");
		status = status == null ? "NORMAL" : status;

		return new MemberDTO(meNo, type, email, pw, name, phoneNum, address, addressDetail, latitude, longitude, emailNotification, null, businessNumber, shortDescription, maxDistance, description, workHours, joinApproval, caNo, point, status);
	}

	/**
	 * 데이터베이스 작업
	 */
	private int memberUpsert(MemberDTO dto, boolean isUpdate) {
		SqlSession sql = sqlSessionFactory.openSession(true);
		MemberMapper Member = sql.getMapper(MemberMapper.class);
		int result;
		if (isUpdate) {
			result = Member.updateMember(dto);
		} else {
			result = Member.insertMember(dto);
		}
		sql.close();
		return result;
	}

	/**
	 * alert창 띄우고 화면 이동
	 */
	private void alertRedirect(HttpServletResponse response, String message, String url) throws IOException {
		response.setContentType("text/html; charset=UTF-8;");
		PrintWriter out = response.getWriter();
		out.println("<script>");
		out.println("alert('" + message + "');");
		if (url.equals("back")) {
			out.println("history.back();");
		} else {
			out.println("location.href='" + url + "';");
		}
		out.println("</script>");
	}

	/**
	 * 상위 카테고리 가져오기
	 */
	public void getCategoryMain(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		SqlSession sql = sqlSessionFactory.openSession();
		CategoryMapper Category = sql.getMapper(CategoryMapper.class);
		ArrayList<CategoryDTO> CategoryMainList = Category.selectCategorys(null); // 메인 카테고리 출력 시 파라미터는 null
		sql.close();
		request.setAttribute("CategoryMainList", CategoryMainList);
	}

	/**
	 * 하위 카테고리 가져오기
	 */
	public void getCategorySub(HttpServletRequest request, HttpServletResponse response, String rootNo) throws ServletException, IOException {

		SqlSession sql = sqlSessionFactory.openSession();
		CategoryMapper Category = sql.getMapper(CategoryMapper.class);
		ArrayList<CategoryDTO> CategorySubList = Category.selectCategorys(rootNo);
		sql.close();
		request.setAttribute("CategorySubList", CategorySubList);
	}

	/**
	 * 회원정보 가져오기
	 */
	public void getMember(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		/* [1] 매개변수 */
		String meNo = request.getParameter("meNo");
		Map<String, Object> params = new HashMap<>();
		params.put("meNo", meNo);

		/* [2] Mapper */
		SqlSession sql = sqlSessionFactory.openSession();
		MemberMapper Member = sql.getMapper(MemberMapper.class);
		MemberDTO dto = Member.selectMember(params);
		sql.close();

		/* [3] 요청에 데이터 설정 */
		request.setAttribute("type", dto.getType());
		request.setAttribute("dto", dto);
	}

	@Override
	public void adminList(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		/* [1] 매개변수 */
		String type = request.getParameter("type");
		type = (type == null || type.isEmpty()) ? "user" : type; // 기본 : 회원 구분 user
		String joinApproval = request.getParameter("joinApproval");
		joinApproval = (joinApproval == null || joinApproval.isEmpty()) ? "all" : joinApproval; // 기본 : 가입 승인 전체(승인/미승인)
		String sortField = request.getParameter("sortField");
		sortField = (sortField == null || sortField.isEmpty()) ? "insert_time" : sortField;
		String sortOrder = request.getParameter("sortOrder");
		sortOrder = (sortOrder == null || sortOrder.isEmpty()) ? "DESC" : sortOrder;
		String rootNo = request.getParameter("rootNo");
		rootNo = (rootNo == null || rootNo.isEmpty()) ? "all" : rootNo;
		String caNo = request.getParameter("caNo");
		caNo = (caNo == null || caNo.isEmpty()) ? "all" : caNo;
		int currentPage = request.getParameter("page") == null ? 1 : Integer.parseInt(request.getParameter("page"));
		int startRow = (currentPage - 1) * recordsPerPage + 1;
		int endRow = startRow + recordsPerPage - 1;

		Map<String, Object> params = new HashMap<>();
		params.put("type", type);
		params.put("joinApproval", joinApproval);
		params.put("sortField", sortField);
		params.put("sortOrder", sortOrder);
		params.put("rootNo", rootNo);
		params.put("caNo", caNo);
		params.put("startRow", startRow); // rownum 시작값
		params.put("endRow", endRow); // rownum 끝값

		/* [2] Mapper */
		SqlSession sql = sqlSessionFactory.openSession();
		MemberMapper Member = sql.getMapper(MemberMapper.class); // MemberMapper 인터페이스를 사용하여 쿼리 실행을 위한 매퍼 객체 생성
		ArrayList<MemberDTO> list = Member.selectMembers(params); // MemberMapper 메서드 호출

		// 페이징
		int totalRecords = Member.countMembers(params); // 해당되는 회원 수 (페이징)
		int totalPages = (int) Math.ceil((double) totalRecords / recordsPerPage);
		int startPage = Math.max(currentPage - 2, 1);
		int endPage = Math.min(currentPage + 2, totalPages);
		endPage = (currentPage == 1 || currentPage == 2) ? Math.min(endPage + 3 - currentPage, totalPages) : endPage;
		startPage = (currentPage == totalPages || currentPage == totalPages - 1) ? Math.max(startPage - 2 + totalPages - currentPage, 1) : startPage;
		params.put("currentPage", currentPage); // 현재 페이지
		params.put("totalPages", totalPages); // 총 페이지 수
		params.put("totalRecords", totalRecords); // 총 레코드 수
		params.put("startPage", startPage); // 표시할 시작 페이지
		params.put("endPage", endPage); // 표시할 마지막 페이지
		sql.close();

		// 카테고리
		if (type.equals("master")) {
			getCategoryMain(request, response);
			if (!rootNo.equals("all")) {
				getCategorySub(request, response, rootNo);
			}
		}

		/* [3] 화면이동 */
		request.setAttribute("list", list);
		for (Map.Entry<String, Object> entry : params.entrySet()) {
			request.setAttribute(entry.getKey(), entry.getValue());
		}
		request.getRequestDispatcher("member_list.jsp").forward(request, response); // 요청 포워드
	}

	@Override
	public void getMasterList(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		/* [1] 매개변수 */
		String rootNo = request.getParameter("rootNo");
		rootNo = (rootNo == null || rootNo.isEmpty()) ? "all" : rootNo;
		String caNo = request.getParameter("caNo");
		caNo = (caNo == null || caNo.isEmpty()) ? "all" : caNo;

		Map<String, Object> params = new HashMap<>();
		params.put("type", "master");
		params.put("joinApproval", "Y");
		params.put("sortField", "insert_time");
		params.put("sortOrder", "DESC");
		params.put("rootNo", rootNo);
		params.put("caNo", caNo);
		params.put("startRow", 1); // rownum 시작값
		params.put("endRow", 999); // rownum 끝값
		// 카테고리 매개변수로 받아서 검색하는 기능 구현 필요

		/* [2] Mapper */
		SqlSession sql = sqlSessionFactory.openSession();
		MemberMapper Member = sql.getMapper(MemberMapper.class);
		ArrayList<MemberDTO> memberList = Member.selectMembers(params);
		sql.close();

		// 카테고리
		getCategoryMain(request, response);

		/* [3] 화면이동 */
		request.setAttribute("gnb", "request");
		request.setAttribute("memberList", memberList);
		request.getRequestDispatcher("master_list.jsp").forward(request, response);

	}

	@Override
	public void adminView(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		// 회원 정보 가져오기
		getMember(request, response);

		// 포워딩
		request.getRequestDispatcher("member_view.jsp").forward(request, response);
	}

	@Override
	public void adminWrite(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		String type = request.getParameter("type");
		request.setAttribute("type", type);
		if (type.equals("master")) {
			getCategoryMain(request, response); // 수리기사 추가할 경우 우선 상위 카테고리만 가져오기
		}

		// 포워딩
		request.getRequestDispatcher("member_edit.jsp").forward(request, response);
	}

	@Override
	public void adminEdit(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		// 회원 정보 가져오기
		getMember(request, response);
		MemberDTO dto = (MemberDTO) request.getAttribute("dto");

		// 수리기사 수정인 경우
		if (dto.getType().equals("master")) {
			// 상위 카테고리 가져오기
			getCategoryMain(request, response);
			// 카테고리가 있을 때만 하위 카테고리 가져오기
			if (dto.getCaRootNo() != null && !dto.getCaRootNo().isEmpty()) {
				getCategorySub(request, response, dto.getCaRootNo());
			}
		}

		// 포워딩
		request.getRequestDispatcher("member_edit.jsp").forward(request, response);

	}

	@Override
	public void adminUpsert(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		MemberDTO dto = createMemberDTO(request, response); // 별도 함수로 처리
		byte[] profilePhoto = getFileBytes(request, response); // 프로필 사진 파일 첨부 처리
		dto.setProfilePhoto(profilePhoto);
		boolean isUpdate = dto.getMeNo() != null && !dto.getMeNo().isEmpty();
		int result = memberUpsert(dto, isUpdate);

		if (result == 1) {
			alertRedirect(response, "정상적으로 " + (isUpdate ? "수정" : "등록") + "되었습니다.", "list.member?type=" + dto.getType());
		}

	}

	@Override
	public void adminUpdateApprove(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		/* [1] 매개변수 */
		String meNo = request.getParameter("meNo");
		String type = request.getParameter("type");

		/* [2] Mapper */
		SqlSession sql = sqlSessionFactory.openSession(true);
		MemberMapper Member = sql.getMapper(MemberMapper.class);
		Member.updateMemberApprove(meNo);
		sql.close();

		/* [3] 화면이동 */
		alertRedirect(response, "승인되었습니다", "list.member?type=" + type);

	}

	@Override
	public void adminDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		/* [1] 매개변수 */
		String meNo = request.getParameter("meNo");
		String type = request.getParameter("type");

		/* [2] Mapper */
		SqlSession sql = sqlSessionFactory.openSession(true);
		MemberMapper Member = sql.getMapper(MemberMapper.class);
		Member.deleteMember(meNo);
		sql.close();

		/* [3] 화면이동 */
		alertRedirect(response, "회원정보가 삭제되었습니다.", "list.member?type=" + type);

	}

	@Override
	public void join(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		getCategoryMain(request, response); // 수리기사 가입일 경우 상위 카테고리 필요
		request.getRequestDispatcher("join.jsp").forward(request, response);
	}

	@Override
	public void register(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		MemberDTO dto = createMemberDTO(request, response); // 별도 함수로 처리
		int result = memberUpsert(dto, false); // insert

		if (result == 1) {
			alertRedirect(response, "회원가입이 완료 되었습니다. 로그인 후 이용해주세요.", "../");
		}
	}

	@Override
	public void login(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		/* [1] 매개변수 */
		String email = request.getParameter("email");
		String pw = request.getParameter("pw");
		String saveEmail = request.getParameter("saveEmail"); // 이메일 저장 체크박스
		Map<String, Object> params = new HashMap<>();
		params.put("email", email);
		params.put("pw", pw);

		/* [2] Mapper */
		SqlSession sql = sqlSessionFactory.openSession();
		MemberMapper Member = sql.getMapper(MemberMapper.class);
		MemberDTO dto = Member.selectMember(params);
		sql.close();

		/* [3] 화면이동 */
		if (dto == null) { // 로그인 실패(email과 pw 일치하는 회원 없음)

			String referrer = request.getHeader("Referer");
			if (referrer.indexOf("loginModal=Y") == -1) {
				referrer += "?loginModal=Y";
			}
			alertRedirect(response, "이메일 또는 비밀번호를 확인하세요.", referrer); // 이전 페이지 보여주기 + 모달 창 다시 띄우기

		} else { // 로그인 성공

			/* 세션(로그인 정보) */
			HttpSession session = request.getSession();
			session.setAttribute("meNo", dto.getMeNo());
			System.out.println(dto.getMeNo());
			session.setAttribute("email", dto.getEmail());
			session.setAttribute("name", dto.getName());
			session.setAttribute("type", dto.getType());
			session.setMaxInactiveInterval(36000); // 10시간

			/* 쿠키(이메일 저장 체크박스) */
			if (saveEmail != null) { // 이메일 저장 체크했을 경우 쿠키 저장

				Cookie saveEmailCookie = new Cookie("saveEmail", email);
				saveEmailCookie.setMaxAge(36000); // 10시간
				saveEmailCookie.setPath(request.getContextPath()); // 쿠키 경로 설정
				response.addCookie(saveEmailCookie);

			} else { // 이메일 저장 체크 해제 시 쿠키 삭제

				Cookie saveEmailCookie = new Cookie("saveEmail", ""); // 빈 값으로 설정
				saveEmailCookie.setMaxAge(0); // 즉시 만료
				saveEmailCookie.setPath(request.getContextPath()); // 쿠키 경로 설정
				response.addCookie(saveEmailCookie);

			}

			response.setContentType("text/html; charset=UTF-8;");
			PrintWriter out = response.getWriter();
			out.println("<script>");
			// out.println("alert('정상적으로 로그인되었습니다.');"); // 임시 주석
			out.println("var url = new URL(document.referrer);");
			out.println("url.searchParams.delete('loginModal');"); // 'loginModal' 파라미터 제거
			out.println("location.href = url.toString();"); // 이전 페이지 보여주기
			out.println("</script>");

		}

	}

	@Override
	public void edit(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		/* [1] 매개변수 */
		HttpSession session = request.getSession();
		String meNo = (String) session.getAttribute("meNo"); // 세션 값 가져오기
		Map<String, Object> params = new HashMap<>();
		params.put("meNo", meNo);

		/* [2] Mapper */
		SqlSession sql = sqlSessionFactory.openSession();
		MemberMapper Member = sql.getMapper(MemberMapper.class);
		MemberDTO dto = Member.selectMember(params);
		// 수리기사 수정인 경우
		if (dto.getType().equals("master")) {
			// 상위 카테고리 가져오기
			getCategoryMain(request, response);
			// 카테고리가 있을 때만 하위 카테고리 가져오기
			if (dto.getCaRootNo() != null && !dto.getCaRootNo().isEmpty()) {
				getCategorySub(request, response, dto.getCaRootNo());
			}
		}
		sql.close();

		/* [3] 화면이동 */
		request.setAttribute("dto", dto);
		request.getRequestDispatcher("profile_edit.jsp").forward(request, response);

	}

	@Override
	public void update(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		MemberDTO dto = createMemberDTO(request, response); // 별도 함수로 처리
		byte[] profilePhoto = getFileBytes(request, response); // 프로필 사진 파일 첨부 처리
		dto.setProfilePhoto(profilePhoto);
		boolean isUpdate = true;
		int result = memberUpsert(dto, isUpdate);

		if (result == 1) {
			alertRedirect(response, "정상적으로 수정되었습니다.", "edit.member");
		}

	}

	@Override
	public void changePwAjax(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		/* [1] 매개변수 */
		HttpSession session = request.getSession();
		String meNo = (String) session.getAttribute("meNo");
		String oldPassword = request.getParameter("pw_old");
		String newPassword = request.getParameter("pw");

		/* [2] Mapper */
		SqlSession sql = sqlSessionFactory.openSession(true);
		MemberMapper memberMapper = sql.getMapper(MemberMapper.class);

		boolean isAvailable = false;
		String msg = "";
		try {
			String storedPassword = memberMapper.getPassword(meNo);
			System.out.println("storedPassword " + storedPassword);
			System.out.println("newPassword" + newPassword);
			if (storedPassword.equals(newPassword)) {
				msg = "현재 비밀번호와 다른 비밀번호로 설정해주세요.";
			} else if (storedPassword != null && storedPassword.equals(oldPassword)) { // 비밀번호 일치
				// 비밀번호 일치 -> 비밀번호 변경
				isAvailable = true;
				MemberDTO dto = new MemberDTO();
				dto.setMeNo(meNo);
				dto.setPw(newPassword);
				int result = memberMapper.updateMember(dto);
				if (result == 1) { // 비밀번호 변경완료
					isAvailable = true;
				} else {
					msg = "비밀번호 변경 실패";
				}
			} else {
				msg = "현재 비밀번호가 일치하지 않습니다.";
			}
		} finally {
			sql.close();
		}

		/* [3] 응답 */
		response.setContentType("application/json; charset=UTF-8");
		PrintWriter out = response.getWriter();
		out.print("{\"available\":" + isAvailable + ", \"msg\":\"" + msg + "\"}");
		out.flush();
	}

	@Override
	public void deleteAccount(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

		/* [1] 매개변수 */
		HttpSession session = request.getSession();
		String meNo = (String) session.getAttribute("meNo");
		String inputPassword = request.getParameter("pw");

		if (meNo == null || inputPassword == null) {
			alertRedirect(response, "잘못된 요청입니다. 다시 시도해 주세요.", "back");
			return;
		}
		/* [2] Mapper */
		SqlSession sqlSession = sqlSessionFactory.openSession();
		MemberMapper memberMapper = sqlSession.getMapper(MemberMapper.class);

		try {
			// DB에서 비밀번호 가져오기
			String storedPassword = memberMapper.getPassword(meNo);

			if (storedPassword != null && storedPassword.equals(inputPassword)) {
				// 비밀번호가 일치하면 회원 정보 삭제
				memberMapper.deleteMember(meNo);
				sqlSession.commit();
				session.invalidate();

				alertRedirect(response, "회원 탈퇴가 완료되었습니다.", request.getContextPath() + "/");

			} else {
				// 비밀번호가 일치하지 않으면 에러 메시지 출력
				alertRedirect(response, "비밀번호가 일치하지 않습니다.", "back");
			}
		} finally {
			sqlSession.close();
		}
	}

	@Override
	public void getMemberById(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

		// 회원 정보 가져오기
		getMember(request, response);

		// 포워딩
		request.setAttribute("gnb", "request");
		request.getRequestDispatcher("master_view.jsp").forward(request, response);

	}

	@Override
	public void getMemberDetails(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

		// 회원 정보 가져오기
		getMember(request, response);

		// 포워딩
		request.setAttribute("gnb", "request");
		request.getRequestDispatcher("request.jsp").forward(request, response);

	}

	@Override
	public void reviewWrite(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		// TODO Auto-generated method stub

	}

	@Override
	public void getMainMaster(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

		SqlSession sql = sqlSessionFactory.openSession();
		MemberMapper Member = sql.getMapper(MemberMapper.class);
		ArrayList<MemberDTO> list = Member.selectMain();
		sql.close();

		SqlSession sql2 = sqlSessionFactory.openSession();
		MemberMapper Member2 = sql2.getMapper(MemberMapper.class);
		ArrayList<MemberDTO> list2 = Member2.selectMainRe();
		sql.close();

		request.setAttribute("list", list);
		request.setAttribute("list2", list2);
		request.getRequestDispatcher("main.jsp").forward(request, response);

	}

	@Override
	public void getMemberPoints(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		HttpSession session = request.getSession();
		String meNo = (String) session.getAttribute("meNo");
		
		
	    try (SqlSession sql = sqlSessionFactory.openSession()) {
	        MemberMapper memberMapper = sql.getMapper(MemberMapper.class);

	        // 포인트 조회
	        Integer points = memberMapper.MemberPoints(meNo);
	     
	        if (points == null) {
	            points = 0; // 포인트가 없으면 기본값 설정
	        }

	        // 포인트 적립 내역 및 사용 내역 조회
	        List<MemberDTO> earnings = memberMapper.getPointEarnings(meNo);
	        List<MemberDTO> spendings = memberMapper.getPointSpendings(meNo);

	        // 결과를 요청 속성에 설정
	        request.setAttribute("points", points);
	        request.setAttribute("earnings", earnings);
	        request.setAttribute("spendings", spendings);

	        // 포워딩
	        request.getRequestDispatcher("point.jsp").forward(request, response);
	    } catch (Exception e) {
	        e.printStackTrace(); // 예외 로그
	        throw new ServletException("데이터베이스 오류", e);
	    }
	}
}