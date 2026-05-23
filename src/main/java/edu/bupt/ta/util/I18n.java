package edu.bupt.ta.util;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public final class I18n {

    public static final String EN = "en";
    public static final String ZH = "zh";

    private static String currentLanguage = EN;
    private static final Map<String, Map<String, String>> translations = new HashMap<>();
    private static Consumer<String> onLanguageChange;

    private I18n() {
    }

    public static void setOnLanguageChange(Consumer<String> callback) {
        onLanguageChange = callback;
    }

    public static void setLanguage(String lang) {
        currentLanguage = lang;
        if (onLanguageChange != null) {
            onLanguageChange.accept(lang);
        }
    }

    public static String getLanguage() {
        return currentLanguage;
    }

    public static String t(String key) {
        if (translations.isEmpty()) {
            initTranslations();
        }
        return translations.getOrDefault(currentLanguage, translations.get(EN)).getOrDefault(key, key);
    }

    public static String t(String key, String defaultVal) {
        if (translations.isEmpty()) {
            initTranslations();
        }
        return translations.getOrDefault(currentLanguage, translations.get(EN)).getOrDefault(key, defaultVal);
    }

    public static String t(String key, String placeholder, String replacement) {
        return t(key, placeholder).replace(placeholder, replacement);
    }

    public static String t(String key, int value) {
        return t(key).replace("{n}", String.valueOf(value));
    }

    public static String t(String key, long value) {
        return t(key).replace("{n}", String.valueOf(value));
    }

    public static String tl(String key, String v1) {
        return t(key).replace("{1}", v1);
    }

    public static String tl(String key, int v1) {
        return t(key).replace("{n}", String.valueOf(v1));
    }

    public static String tl(String key, long v1) {
        return t(key).replace("{n}", String.valueOf(v1));
    }

    public static String tl(String key, int v1, int v2) {
        return t(key).replace("{n1}", String.valueOf(v1)).replace("{n2}", String.valueOf(v2));
    }

    public static String tl(String key, long v1, int v2) {
        return t(key).replace("{n1}", String.valueOf(v1)).replace("{n2}", String.valueOf(v2));
    }

    public static String tl(String key, int v1, long v2, int v3) {
        return t(key).replace("{n1}", String.valueOf(v1)).replace("{n2}", String.valueOf(v2)).replace("{n3}", String.valueOf(v3));
    }

    public static void initTranslations() {
        Map<String, String> en = new HashMap<>();
        Map<String, String> zh = new HashMap<>();

        // ===================== GENERAL =====================
        put(en, zh, "ok", "OK", "确定");
        put(en, zh, "cancel", "Cancel", "取消");
        put(en, zh, "save", "Save", "保存");
        put(en, zh, "reset", "Reset", "重置");
        put(en, zh, "close", "Close", "关闭");
        put(en, zh, "error", "Error", "错误");
        put(en, zh, "success", "Success", "成功");
        put(en, zh, "confirm", "Confirm", "确认");
        put(en, zh, "yes", "Yes", "是");
        put(en, zh, "no", "No", "否");
        put(en, zh, "back", "Back", "返回");
        put(en, zh, "next", "Next", "下一步");
        put(en, zh, "loading", "Loading...", "加载中...");

        // ===================== SETTINGS =====================
        put(en, zh, "settings", "Settings", "设置");
        put(en, zh, "settings_subtitle", "Customize your preferences", "自定义您的偏好设置");
        put(en, zh, "language", "Language", "语言");
        put(en, zh, "font_size", "Font Size", "字体大小");
        put(en, zh, "change_password", "Change Password", "修改密码");
        put(en, zh, "change_username", "Change Username", "修改用户名");
        put(en, zh, "current_password", "Current Password", "当前密码");
        put(en, zh, "new_username", "New Username", "新用户名");
        put(en, zh, "new_password", "New Password", "新密码");
        put(en, zh, "confirm_password", "Confirm Password", "确认密码");
        put(en, zh, "small", "Small", "小");
        put(en, zh, "medium", "Medium", "中");
        put(en, zh, "large", "Large", "大");
        put(en, zh, "english", "English", "English");
        put(en, zh, "chinese", "中文", "中文");
        put(en, zh, "settings_saved", "Settings saved successfully.", "设置保存成功。");
        put(en, zh, "password_mismatch", "Passwords do not match.", "两次输入的密码不一致。");
        put(en, zh, "incorrect_password", "Incorrect current password.", "当前密码错误。");
        put(en, zh, "username_required", "Username cannot be empty.", "用户名不能为空。");
        put(en, zh, "password_too_short", "Password must be at least 6 characters.", "密码长度至少为6位。");
        put(en, zh, "update_success", "Update successful.", "更新成功。");
        put(en, zh, "username_taken", "Username already taken.", "用户名已被占用。");

        // ===================== MAIN SHELL =====================
        put(en, zh, "recruitment_system", "Recruitment System", "招聘系统");
        put(en, zh, "bupt_is_recruitment", "BUPT IS RECRUITMENT", "BUPT IS 招聘系统");
        put(en, zh, "ta_edition", "TA EDITION", "TA 版本");
        put(en, zh, "mo_edition", "MO EDITION", "MO 版本");
        put(en, zh, "admin_edition", "ADMIN EDITION", "管理员版本");
        put(en, zh, "dashboard", "Dashboard", "首页");
        put(en, zh, "browse_jobs", "Browse Jobs", "浏览职位");
        put(en, zh, "my_applications", "My Applications", "我的申请");
        put(en, zh, "my_cv", "My CV", "我的简历");
        put(en, zh, "job_management", "Job Management", "职位管理");
        put(en, zh, "applicant_list", "Applicant List", "申请人列表");
        put(en, zh, "profile", "Profile", "个人资料");
        put(en, zh, "jobs", "Jobs", "职位");
        put(en, zh, "applications", "Applications", "申请");
        put(en, zh, "help_center", "Help Center", "帮助中心");
        put(en, zh, "logout", "Logout", "退出登录");
        put(en, zh, "notifications", "Notifications", "通知");
        put(en, zh, "mark_all_read", "Mark all read", "全部标为已读");
        put(en, zh, "clear_all", "Clear All Notifications", "清除所有通知");
        put(en, zh, "no_notifications", "No notifications", "暂无通知");
        put(en, zh, "spring_semester_2026", "Spring Semester 2026", "2026年春季学期");
        put(en, zh, "bupt_international_school", "BUPT International School", "北京邮电大学国际学院");

        // ===================== LOGIN =====================
        put(en, zh, "portal_login", "Portal Login", "登录入口");
        put(en, zh, "enter_credentials", "Enter your university credentials to continue", "请输入您的大学账号密码以继续");
        put(en, zh, "university_id", "University ID / Username", "学号 / 用户名");
        put(en, zh, "eg_2023211000", "e.g. 2023211000", "例如 2023211000");
        put(en, zh, "forgot_password", "Forgot password?", "忘记密码？");
        put(en, zh, "login_to_portal", "LOGIN TO PORTAL", "登录");
        put(en, zh, "register", "REGISTER", "注册");
        put(en, zh, "help_center_link", "HELP CENTER", "帮助中心");
        put(en, zh, "privacy_link", "PRIVACY", "隐私政策");
        put(en, zh, "username_required_msg", "Username is required.", "用户名不能为空。");
        put(en, zh, "username_not_found", "User not found.", "用户不存在。");
        put(en, zh, "password_required_msg", "Password cannot be empty.", "密码不能为空。");
        put(en, zh, "account_inactive", "Account is inactive.", "账号已被禁用。");
        put(en, zh, "incorrect_password_msg", "Incorrect password.", "密码错误。");
        put(en, zh, "bupt_title", "BUPT International School", "北京邮电大学");
        put(en, zh, "ta_recruitment_title", "Teaching Assistant\nRecruitment System", "教学助理\n招聘系统");
        put(en, zh, "secure_portal", "Secure Academic Portal for Students & Faculty", "师生安全学术门户");
        put(en, zh, "copyright", "© 2026 Beijing University of Posts and Telecommunications. All rights reserved.", "© 2026 北京邮电大学 版权所有");
        put(en, zh, "reset_password_title", "Reset Password", "重置密码");
        put(en, zh, "verification_code", "Verification Code", "验证码");
        put(en, zh, "verification_required", "Verification code is required.", "验证码不能为空。");
        put(en, zh, "verification_incorrect", "Verification code is incorrect.", "验证码错误。");
        put(en, zh, "new_password_required", "New password is required.", "新密码不能为空。");
        put(en, zh, "password_min_length", "Password must be at least 8 characters.", "密码长度至少为8位。");
        put(en, zh, "password_mismatch_msg", "Password and confirmation do not match.", "两次密码输入不一致。");
        put(en, zh, "password_reset_success", "Password has been updated. Please log in with the new password.", "密码已更新，请使用新密码登录。");
        put(en, zh, "create_account_title", "Create Account", "创建账户");
        put(en, zh, "full_name", "Full Name", "姓名");
        put(en, zh, "student_id", "Student ID", "学号");
        put(en, zh, "major_programme", "Major / Programme", "专业");
        put(en, zh, "email_address", "Email Address", "电子邮箱");
        put(en, zh, "phone_number", "Phone Number", "联系电话");
        put(en, zh, "select_campus", "Select Campus", "选择校区");
        put(en, zh, "select_academic_year", "Select Academic Year", "选择学年");
        put(en, zh, "title_label", "Title", "职称");
        put(en, zh, "department_label", "Department", "院系");
        put(en, zh, "contact_email", "Contact Email", "联系邮箱");
        put(en, zh, "select_role", "Select Role", "选择角色");
        put(en, zh, "registration_success", "Registration Successful", "注册成功");
        put(en, zh, "registration_success_msg", "Account created. Please log in with your new credentials.", "账号已创建，请使用新账号登录。");

        // ===================== JOB EDITOR VALIDATION =====================
        put(en, zh, "title_is_required", "Job title is required.", "职位名称不能为空。");
        put(en, zh, "module_code_is_required", "Module code is required.", "模块代码不能为空。");
        put(en, zh, "module_name_is_required", "Module name is required.", "模块名称不能为空。");
        put(en, zh, "semester_is_required", "Semester is required.", "学期不能为空。");
        put(en, zh, "positions_must_be_positive", "Positions must be greater than zero.", "名额必须大于零。");
        put(en, zh, "deadline_is_required", "Deadline is required.", "截止日期不能为空。");
        put(en, zh, "select_both_hour_minute", "Please select both hour and minute.", "请选择小时和分钟。");
        put(en, zh, "open_job_future_deadline", "Open jobs must have a future deadline.", "开放职位的截止时间必须是未来时间。");
        put(en, zh, "organiser_id_required", "An organiser must be selected.", "必须选择负责人。");
        put(en, zh, "select_at_least_one_campus", "Please select at least one campus.", "请至少选择一个校区。");

        // Registration validation
        put(en, zh, "username_required_v", "Username is required.", "用户名不能为空。");
        put(en, zh, "username_format", "Username must be 3-30 characters and contain only letters, numbers, or underscores.", "用户名长度3-30位，仅限字母、数字和下划线。");
        put(en, zh, "username_exists", "Username already exists.", "用户名已存在。");
        put(en, zh, "password_required_v", "Password is required.", "密码不能为空。");
        put(en, zh, "password_length", "Password must contain at least 8 characters.", "密码长度至少为8位。");
        put(en, zh, "password_uppercase", "Password must contain at least one uppercase letter.", "密码必须包含至少一个大写字母。");
        put(en, zh, "password_lowercase", "Password must contain at least one lowercase letter.", "密码必须包含至少一个小写字母。");
        put(en, zh, "password_number", "Password must contain at least one number.", "密码必须包含至少一个数字。");
        put(en, zh, "password_special", "Password must contain at least one special character.", "密码必须包含至少一个特殊字符。");
        put(en, zh, "fullname_required", "Full name is required.", "姓名不能为空。");
        put(en, zh, "fullname_format", "Full name must be 2-60 characters.", "姓名长度2-60位。");
        put(en, zh, "studentid_required", "Student ID is required.", "学号不能为空。");
        put(en, zh, "studentid_format", "Student ID must contain only digits and be 6-20 characters.", "学号仅限数字，长度6-20位。");
        put(en, zh, "email_required", "Email address is required.", "邮箱不能为空。");
        put(en, zh, "email_invalid", "Email format is invalid.", "邮箱格式不正确。");
        put(en, zh, "academic_year_required", "Academic year is required.", "学年不能为空。");
        put(en, zh, "major_required", "Major is required.", "专业不能为空。");
        put(en, zh, "campus_required", "Campus is required.", "校区不能为空。");
        put(en, zh, "title_required", "Title is required.", "职称不能为空。");
        put(en, zh, "dept_required", "Department is required.", "院系不能为空。");
        put(en, zh, "contact_email_required", "Contact email is required.", "联系邮箱不能为空。");

        // ===================== TA DASHBOARD =====================
        put(en, zh, "welcome_back", "Welcome back, {name}", "欢迎回来，{name}");
        put(en, zh, "active_applications", "You have {n} active application(s) for this recruitment cycle.", "您有 {n} 条正在进行的申请。");
        put(en, zh, "view_schedule", "View Schedule", "查看进度");
        put(en, zh, "browse_new_jobs", "Browse New Jobs", "浏览新职位");
        put(en, zh, "complete", "COMPLETE", "已完成");
        put(en, zh, "incomplete", "INCOMPLETE", "未完成");
        put(en, zh, "fields_filled", "/ {n} fields", "/ {n} 个字段");
        put(en, zh, "all_fields_filled", "All fields filled", "所有字段已填写");
        put(en, zh, "fields_remaining", "{n} field(s) remaining", "还有 {n} 个字段未填写");
        put(en, zh, "uploaded", "UPLOADED", "已上传");
        put(en, zh, "add_cv", "Add your latest CV", "上传您的最新简历");
        put(en, zh, "completion_percent", "{n}% completion", "{n}% 完成度");
        put(en, zh, "active", "ACTIVE", "进行中");
        put(en, zh, "no_submissions_yet", "No submissions yet", "暂无申请记录");
        put(en, zh, "track_progress", "Track current recruitment progress", "跟踪当前招聘进度");
        put(en, zh, "recent_open_jobs", "Recent Open Jobs", "最近开放职位");
        put(en, zh, "view_all", "View All", "查看全部");
        put(en, zh, "no_recent_jobs", "No recent jobs available.", "暂无最近职位。");
        put(en, zh, "recommended_for_you", "Recommended for You", "为您推荐");
        put(en, zh, "ai_auto_match", "AI Auto Match", "AI 自动匹配");
        put(en, zh, "recommended_skill_match", "Recommended based on your skills and workload", "根据您的技能和工作量推荐");
        put(en, zh, "hours_per_week", "", "");
        put(en, zh, "deadline_label", "Deadline: ", "截止日期：");
        put(en, zh, "match_rate_percent", "{n}% Match", "{n}% 匹配度");
        put(en, zh, "quick_actions", "Quick Actions", "快捷操作");
        put(en, zh, "complete_profile", "Complete Profile", "完善资料");
        put(en, zh, "upload_cv_portfolio", "Upload CV / Portfolio", "上传简历");
        put(en, zh, "status_check", "Status Check", "状态检查");
        put(en, zh, "missing_documents", "Missing Documents", "缺少文件");
        put(en, zh, "profile_incomplete", "Profile Incomplete", "资料不完整");
        put(en, zh, "workload_warning", "Workload Warning", "工作量警告");
        put(en, zh, "all_set", "All Set", "一切就绪");
        put(en, zh, "missing_cv_msg", "Please upload your CV or portfolio before applying widely.", "请上传您的简历后再广泛申请职位。");
        put(en, zh, "profile_missing_sections", "Resume sections missing: {sections}", "简历部分缺失：{sections}");
        put(en, zh, "profile_incomplete_msg", "Update your applicant profile to reach full completion.", "请完善您的申请资料以达到完整状态。");
        put(en, zh, "workload_warning_msg", "Current workload is close to the policy limit.", "当前工作量接近政策上限。");
        put(en, zh, "all_set_msg", "Your documents and workload status look healthy.", "您的文件和 workload 状态良好。");
        put(en, zh, "recruitment_deadlines", "Recruitment Deadlines", "招聘截止日期");
        put(en, zh, "no_upcoming_deadlines", "No upcoming deadlines right now.", "暂无即将到期的截止日期。");
        put(en, zh, "select_job_to_preview", "Select a job card on the left to preview details.", "请在左侧选择一个职位卡片以预览详情。");
        put(en, zh, "apply_failed", "Apply Failed", "申请失败");
        put(en, zh, "apply_success", "Application Submitted", "申请已提交");
        put(en, zh, "apply_success_msg", "Your application was submitted successfully.", "您的申请已成功提交。");
        put(en, zh, "cancel_success", "Cancel Success", "取消成功");
        put(en, zh, "cancel_success_msg", "Application cancelled successfully.", "申请已成功取消。");
        put(en, zh, "cancel_failed", "Cancel Failed", "取消失败");
        put(en, zh, "profile_not_found", "Profile not found for current TA account. Please complete your profile first.", "未找到当前 TA 账号的资料，请先完善您的资料。");

        // ===================== MO DASHBOARD & KPI =====================
        put(en, zh, "welcome_back_mo", "Welcome back, {1}", "欢迎回来，{1}");
        put(en, zh, "managing_job_posts", "Managing {n1} job post(s) — {n2} open, {n3} application(s)", "正在管理 {n1} 个职位 — {n2} 个开放，{n3} 条申请");
        put(en, zh, "no_job_posts_created", "No job posts created yet.", "暂无职位发布。");
        put(en, zh, "managed_jobs", "Managed Jobs", "已管理职位");
        put(en, zh, "currently_open", "{n} currently open", "{n} 个正在开放");
        put(en, zh, "no_postings_created_yet", "No postings created yet", "暂无发布");
        put(en, zh, "active_applications", "Active Applications", "活跃申请");
        put(en, zh, "open_unfilled_expired", "Open / Unfilled / Expired", "开放/未录取/已过期");
        put(en, zh, "draft_jobs", "Draft Jobs", "草稿职位");
        put(en, zh, "all_jobs_publishable", "All jobs publishable", "所有职位可发布");
        put(en, zh, "not_visible_until_published", "Not visible until published", "发布前不可见");
        put(en, zh, "under_review_count", "Under Review", "审核中");
        put(en, zh, "under_review_count2", "Under Review ({n})", "审核中 ({n})");
        put(en, zh, "no_active_applications", "No active applications", "无活跃申请");
        put(en, zh, "in_screening", "{n1} in screening ({n2}%)", "{n1} 正在审核中（{n2}%）");
        put(en, zh, "accepted_count2", "Accepted ({n})", "已接受 ({n})");
        put(en, zh, "all_count", "All ({n})", "全部 ({n})");
        put(en, zh, "draft_jobs_pending_desc", "{n} draft job(s) pending review", "{n} 个草稿职位待审核");
        put(en, zh, "open_no_applicants_desc", "{n} open position(s) with no applicants yet", "{n} 个开放职位暂无申请人");
        put(en, zh, "deadline_passed", "Deadline passed", "已过期");
        put(en, zh, "urgent_closes", "Urgent: Closes {1}", "紧急：{1} 截止");
        put(en, zh, "approaching_closes", "Approaching: {1}", "即将截止：{1}");
        put(en, zh, "deadline_closes", "Closes {1}", "{1} 截止");
        put(en, zh, "applicants_label", "{n} applicant(s)", "{n} 个申请人");
        put(en, zh, "job_management_sidebar", "Job Management", "职位管理");
        put(en, zh, "applicant_list_sidebar", "Applicant List", "申请人列表");
        put(en, zh, "recent_jobs_3_months", "Recent Jobs (Last 3 Months)", "最近职位（近3个月）");
        put(en, zh, "created_last_3_months", "Created in the last 3 months", "近3个月内创建");
        put(en, zh, "no_draft_jobs_pending", "No draft jobs pending", "无待发布草稿职位");
        put(en, zh, "unpublished_job_posts", "Unpublished job posts", "未发布的职位");
        put(en, zh, "quick_actions_mo", "Quick Actions", "快捷操作");
        put(en, zh, "jump_workflows", "Jump to your common workflows", "跳转到常用工作流程");
        put(en, zh, "manage_jobs_btn", "Manage Jobs", "管理职位");
        put(en, zh, "review_applicants_btn", "Review Applicants", "审核申请人");
        put(en, zh, "update_profile_btn", "Update Profile", "更新资料");
        put(en, zh, "recruitment_deadlines_mo", "Recruitment Deadlines", "招聘截止日期");
        put(en, zh, "no_active_deadlines", "No active deadlines", "无活跃截止日期");
        put(en, zh, "only_open_expired", "Only open or expired jobs shown", "仅显示开放或已过期职位");
        put(en, zh, "status_check_mo", "Status Check", "状态检查");
        put(en, zh, "draft_jobs_pending", "Draft Jobs Pending", "待发布草稿职位");
        put(en, zh, "open_jobs_without_applicants", "Open Jobs Without Applicants", "暂无申请人的开放职位");
        put(en, zh, "all_clear_mo", "All Clear", "一切正常");
        put(en, zh, "no_management_issues", "No management issues detected.", "未检测到管理问题。");
        put(en, zh, "untitled_job", "Untitled Job", "无标题职位");
        put(en, zh, "module_name", "Module Name", "模块名称");
        put(en, zh, "professor", "Professor", "教授");
        put(en, zh, "no_deadline_set", "No deadline set", "未设置截止日期");

        // ===================== JOB EDITOR =====================
        put(en, zh, "create_job_post", "Create Job Post", "创建职位发布");
        put(en, zh, "edit_job_post", "Edit Job Post", "编辑职位发布");
        put(en, zh, "publish_job", "Publish Job", "发布职位");
        put(en, zh, "save_draft", "Save Draft", "保存草稿");
        put(en, zh, "update_job", "Update Job", "更新职位");
        put(en, zh, "basic_job_information", "Basic Job Information", "基础职位信息");
        put(en, zh, "live_preview_summary", "Live Preview Summary", "实时预览摘要");
        put(en, zh, "job_title_label", "Job Title", "职位名称");
        put(en, zh, "module_code", "Module Code", "模块代码");
        put(en, zh, "job_type", "Job Type", "职位类型");
        put(en, zh, "positions_label", "Positions", "名额");
        put(en, zh, "organiser", "Organiser", "负责人");
        put(en, zh, "job_description_responsibilities", "Job Description / Responsibilities", "职位描述 / 主要职责");
        put(en, zh, "required_skills_comma", "Required Skills (comma-separated)", "必需技能（逗号分隔）");
        put(en, zh, "preferred_skills_comma", "Preferred Skills (comma-separated)", "优先技能（逗号分隔）");
        put(en, zh, "essential_technical_skills", "Essential Technical Skills", "核心技术技能");
        put(en, zh, "minimum_academic_grade", "Minimum Academic Grade", "最低学业成绩");
        put(en, zh, "tip_required_skill", "Add at least one required skill to help candidates understand the role.", "请至少添加一项必需技能，以帮助申请人了解岗位要求。");
        put(en, zh, "job_description_upper", "JOB DESCRIPTION", "职位描述");
        put(en, zh, "key_responsibilities_upper", "KEY RESPONSIBILITIES", "主要职责");
        put(en, zh, "module_info_label", "MODULE INFO", "模块信息");
        put(en, zh, "deadline_upper", "DEADLINE", "截止日期");
        put(en, zh, "code", "Code", "代码");
        put(en, zh, "term", "Term", "学期");
        put(en, zh, "seats", "Seats", "名额");
        put(en, zh, "hour", "Hour", "小时");
        put(en, zh, "minute", "Minute", "分钟");
        put(en, zh, "department_label3", "Department", "部门");
        put(en, zh, "no_job_description_provided", "No job description provided.", "暂无职位描述。");
        put(en, zh, "spring_semester", "Spring Semester", "春季学期");
        put(en, zh, "tbd", "TBD", "待定");
        put(en, zh, "select_organiser", "Select an organiser", "请选择负责人");
        put(en, zh, "add_skill", "Add Skill", "添加技能");
        put(en, zh, "add_one_required_skill", "Add one required skill", "添加一项必需技能");
        put(en, zh, "skill_name_colon", "Skill name:", "技能名称：");
        put(en, zh, "a_90_plus", "A (90+)", "A（90分及以上）");
        put(en, zh, "b_plus_85_plus", "B+ (85+)", "B+（85分及以上）");

        // ===================== JOB BROWSER =====================
        put(en, zh, "browse_opportunities", "Browse Opportunities", "浏览机会");
        put(en, zh, "search_keyword", "Search by Keyword (e.g. CS101, Python, Web Dev)", "按关键词搜索（如 CS101、Python、Web Dev）");
        put(en, zh, "search_btn", "SEARCH", "搜索");
        put(en, zh, "status_filter", "Status", "状态");
        put(en, zh, "open_status", "OPEN", "开放");
        put(en, zh, "closed_status", "CLOSED", "已关闭");
        put(en, zh, "expired_status", "EXPIRED", "已过期");
        put(en, zh, "job_type_filter", "Job Type", "职位类型");
        put(en, zh, "module_code_filter", "Module Code", "模块代码");
        put(en, zh, "skills_filter", "Skills", "技能要求");
        put(en, zh, "deadline_filter", "Deadline", "截止日期");
        put(en, zh, "within_7_days", "Within 7 days", "7天内");
        put(en, zh, "within_30_days", "Within 30 days", "30天内");
        put(en, zh, "within_90_days", "Within 90 days", "90天内");
        put(en, zh, "clear_filters", "CLEAR FILTERS", "清除筛选");
        put(en, zh, "open_positions", "Open Positions", "开放职位");
        put(en, zh, "applied_status", "Applied", "已申请");
        put(en, zh, "cancelled_status", "Cancelled", "已取消");
        put(en, zh, "under_review_status", "Under Review", "审核中");
        put(en, zh, "rejected_status", "Rejected", "已拒绝");
        put(en, zh, "accepted_status", "Accepted", "已接受");
        put(en, zh, "accepted_short", "Accepted", "已接受");
        put(en, zh, "post_single", "1 Post", "1 个职位");
        put(en, zh, "posts_plural", "{n} Posts", "{n} 个职位");
        put(en, zh, "apply_fail_title", "Apply Failed", "申请失败");

        // ===================== JOB DETAIL =====================
        put(en, zh, "select_position", "Select a position", "请选择一个职位");
        put(en, zh, "choose_job_card", "Choose a job card on the left to preview details.", "请在左侧选择一个职位卡片以预览详情。");
        put(en, zh, "action_required", "Action Required: Profile Incomplete", "操作提示：资料不完整");
        put(en, zh, "complete_profile_first", "Please complete your profile and upload your CV to apply for this position.", "请完善您的个人资料并上传简历后再申请此职位。");
        put(en, zh, "complete_profile_btn", "Complete Profile", "完善资料");
        put(en, zh, "available_seats", "AVAILABLE", "开放名额");
        put(en, zh, "seats_label", "SEATS", "名额");
        put(en, zh, "application_deadline", "APPLICATION", "申请");
        put(en, zh, "deadline_label", "DEADLINE", "截止日期");
        put(en, zh, "job_description", "JOB DESCRIPTION", "职位描述");
        put(en, zh, "key_responsibilities", "KEY RESPONSIBILITIES", "主要职责");
        put(en, zh, "module_info", "MODULE INFO", "模块信息");
        put(en, zh, "code_label", "CODE", "代码");
        put(en, zh, "professor_label", "PROFESSOR", "教授");
        put(en, zh, "campus_label", "CAMPUS", "校区");
        put(en, zh, "term_label", "TERM", "学期");
        put(en, zh, "minimum_grade_label", "MINIMUM GRADE", "最低成绩要求");
        put(en, zh, "no_job_description", "No job description provided.", "暂无职位描述。");
        put(en, zh, "no_responsibilities", "Select an open position to view responsibilities and requirements.", "请选择一个开放职位以查看职责和要求。");
        put(en, zh, "apply_now", "APPLY NOW", "立即申请");
        put(en, zh, "cancel_application", "CANCEL APPLICATION", "取消申请");
        put(en, zh, "shahe_campus", "Shahe Campus", "沙河校区");

        // ===================== MY APPLICATIONS =====================
        put(en, zh, "status_overview", "STATUS OVERVIEW", "状态概览");
        put(en, zh, "all_applications", "All Applications", "全部申请");
        put(en, zh, "application_details", "Application Details", "申请详情");
        put(en, zh, "current_stage", "CURRENT STAGE", "当前阶段");
        put(en, zh, "reviewer_feedback", "Reviewer Feedback", "审核反馈");
        put(en, zh, "timeline_label", "Timeline", "时间线");
        put(en, zh, "application_submitted", "Application Submitted", "已提交申请");
        put(en, zh, "application_under_review", "Application Under Review", "审核中");
        put(en, zh, "final_decision", "Final Decision", "最终决定");
        put(en, zh, "view_job_details", "View Job Details", "查看职位详情");
        put(en, zh, "view_full_application", "View Full Application", "查看完整申请");
        put(en, zh, "withdraw_application", "Withdraw Application", "撤回申请");
        put(en, zh, "no_applications_yet", "No applications yet", "暂无申请记录");
        put(en, zh, "no_applications_subtitle", "You have not applied to any TA positions yet. Start by browsing open roles.", "您还没有申请任何 TA 职位，快去浏览开放职位吧。");
        put(en, zh, "browse_open_jobs", "Browse Open Jobs", "浏览开放职位");
        put(en, zh, "refresh_btn", "Refresh", "刷新");
        put(en, zh, "use_browse_page", "Use the Browse Jobs page from the left navigation.", "请使用左侧导航中的浏览职位页面。");
        put(en, zh, "no_applications_status", "No applications in this status", "该状态下无申请记录");
        put(en, zh, "try_switching", "Try switching to another status or clear filters.", "请尝试切换到其他状态或清除筛选条件。");
        put(en, zh, "unable_to_withdraw", "Unable to withdraw", "无法撤回");
        put(en, zh, "only_submitted_withdraw", "Only submitted applications can be withdrawn.", "仅已提交的申请可以撤回。");
        put(en, zh, "withdraw_confirm_title", "Withdraw Application", "撤回申请");
        put(en, zh, "withdraw_confirm_msg", "Are you sure you want to withdraw this application?", "确定要撤回此申请吗？");
        put(en, zh, "application_withdrawn", "Application Withdrawn", "申请已撤回");
        put(en, zh, "withdraw_success_msg", "Your application has been withdrawn successfully.", "您的申请已成功撤回。");
        put(en, zh, "application_detail_title", "Application Detail", "申请详情");

        // ===================== RESUME INFO =====================
        put(en, zh, "resume_information", "Resume Information", "简历信息");
        put(en, zh, "maintain_resume_data", "Maintain structured CV data for matching and workload analysis.", "维护结构化简历数据，用于匹配和工作量分析。");
        put(en, zh, "relevant_modules", "Relevant Modules (comma separated)", "相关模块（逗号分隔）");
        put(en, zh, "technical_skills", "Technical Skills (comma separated)", "技术技能（逗号分隔）");
        put(en, zh, "language_skills", "Language Skills (comma separated)", "语言技能（逗号分隔）");
        put(en, zh, "availability_label", "Availability (comma separated)", "可用时间（逗号分隔）");
        put(en, zh, "max_weekly_hours", "Load Capacity", "负载容量");
        put(en, zh, "experience_label", "Experience", "经验");
        put(en, zh, "personal_statement_label", "Personal Statement", "个人陈述");
        put(en, zh, "save_resume", "Save Resume", "保存简历");
        put(en, zh, "matching_quality_hint", "Matching Quality Hint", "匹配质量提示");
        put(en, zh, "improve_match_hint", "Include required skills and availability windows to improve explainable match score.", "包含所需技能和可用时间段，以提高可解释的匹配分数。");
        put(en, zh, "resume_saved", "Resume Saved", "简历已保存");
        put(en, zh, "resume_saved_msg", "Resume saved successfully.", "简历保存成功。");

        // ===================== MO DASHBOARD =====================
        put(en, zh, "professor", "Professor", "教授");
        put(en, zh, "module_name", "Module Name", "模块名称");
        put(en, zh, "job_status_open", "OPEN", "开放");
        put(en, zh, "job_status_draft", "DRAFT", "草稿");
        put(en, zh, "job_status_closed", "CLOSED", "已关闭");
        put(en, zh, "job_status_expired", "EXPIRED", "已过期");
        put(en, zh, "final_decision", "Final Decision", "最终决定");
        put(en, zh, "courses_i_teach", "Courses I Teach", "我教授的课程");
        put(en, zh, "no_courses_found", "No courses found.", "未找到课程。");
        put(en, zh, "ta_position_plural", "TA positions", "个助教岗位");

        // ===================== MY APPLICATIONS =====================
        put(en, zh, "just_now", "just now", "刚刚");
        put(en, zh, "mins_ago_unit", "{n}m ago", "{n} 分钟前");
        put(en, zh, "hours_ago_unit", "{n}h ago", "{n} 小时前");
        put(en, zh, "days_ago_unit", "{n}d ago", "{n} 天前");
        put(en, zh, "day_singular", "1d ago", "1 天前");
        put(en, zh, "weeks_ago_unit", "{n}w ago", "{n} 周前");
        put(en, zh, "week_singular", "1w ago", "1 周前");
        put(en, zh, "fall_term", "Fall", "秋季");
        put(en, zh, "spring_term", "Spring", "春季");
        put(en, zh, "module_organiser_label", "Module Organiser", "课程负责人");
        put(en, zh, "application_detail_title", "Application Detail", "申请详情");
        put(en, zh, "final_decision_expected", "Expected within 2 weeks", "预计两周内通知");
        put(en, zh, "semester_label", "Semester", "学期");

        // ===================== MY CV =====================
        put(en, zh, "cv_management", "CV Management", "简历管理");
        put(en, zh, "upload_manage_cv", "Upload and manage your CV file", "上传和管理您的简历文件");
        put(en, zh, "basic_information_upper", "Basic Information", "基本信息");
        put(en, zh, "full_name_upper", "Full Name", "姓名");
        put(en, zh, "student_id_upper", "Student ID", "学号");
        put(en, zh, "degree_program_upper", "Degree / Programme", "学位 / 专业");
        put(en, zh, "email_upper", "Email", "电子邮箱");
        put(en, zh, "cv_completion", "CV Completion", "简历完成度");
        put(en, zh, "cv_complete_percent", "{n}% complete", "{n}% 完成");
        put(en, zh, "phone_upper", "Phone", "电话");
        put(en, zh, "select_campus", "Campus", "校区");
        put(en, zh, "accept_cross_campus_upper", "Accept Cross-Campus", "接受跨校区");
        put(en, zh, "academic_year_upper", "Academic Year", "学年");
        put(en, zh, "year_label", "Year {n}", "第 {n} 年");
        put(en, zh, "yes_option", "Yes", "是");
        put(en, zh, "no_option", "No", "否");
        put(en, zh, "upload_new_cv", "Upload New CV", "上传新简历");
        put(en, zh, "supported_formats_pdf_docx", "Supported formats: PDF, DOCX", "支持格式：PDF、DOCX");
        put(en, zh, "click_to_upload_drag_drop", "Click to upload or drag and drop your CV file here", "点击上传或拖拽简历文件到此处");
        put(en, zh, "file_auto_parsed_profile", "File will be auto-parsed to populate your profile", "文件将自动解析以填充您的个人资料");
        put(en, zh, "select_file", "Select File", "选择文件");
        put(en, zh, "cv_uploaded_successfully", "CV Uploaded", "简历已上传");
        put(en, zh, "cv_in_progress", "CV In Progress", "简历上传中");
        put(en, zh, "verification_complete", "Verification complete", "验证完成");
        put(en, zh, "awaiting_completion", "Awaiting completion", "等待完善");
        put(en, zh, "next_steps", "Next Steps", "后续步骤");
        put(en, zh, "browse_available_positions", "Browse available positions", "浏览可用职位");
        put(en, zh, "complete_profile_details", "Complete your profile details", "完善您的个人资料");
        put(en, zh, "cv_guidelines_applicants", "CV Guidelines for Applicants", "申请者简历指南");
        put(en, zh, "ensure_gpa", "Ensure your CV includes your GPA and academic background.", "请确保简历中包含您的GPA和学术背景。");
        put(en, zh, "list_previous_ta", "List any previous Teaching Assistant experience.", "请列出您之前的教学助理经历。");
        put(en, zh, "include_proficiency", "Include proficiency in relevant programming languages or tools.", "请包含相关编程语言或工具的熟练程度。");
        put(en, zh, "keep_file_size_small", "Keep your file size under 5MB.", "请将文件大小控制在5MB以内。");
        put(en, zh, "open_cv_file", "Open CV file", "打开简历文件");
        put(en, zh, "delete_cv_file", "Delete CV file", "删除简历文件");
        put(en, zh, "not_updated_yet", "Not updated yet", "尚未更新");
        put(en, zh, "updated_just_now", "Updated just now", "刚刚更新");
        put(en, zh, "updated_mins_ago", "Updated {n} mins ago", "{n} 分钟前更新");
        put(en, zh, "updated_hours_ago", "Updated {n} hours ago", "{n} 小时前更新");
        put(en, zh, "updated_1_hour_ago", "Updated 1 hour ago", "1 小时前更新");
        put(en, zh, "select_cv_file", "Select CV File", "选择简历文件");
        put(en, zh, "cv_uploaded_label", "CV Uploaded", "简历已上传");
        put(en, zh, "cv_uploaded_success_desc", "Your CV file was uploaded successfully.", "您的简历文件上传成功。");
        put(en, zh, "cv_not_found", "CV Not Found", "未找到简历");
        put(en, zh, "no_uploaded_cv_exists", "No uploaded CV file exists for this account.", "此账号不存在已上传的简历文件。");
        put(en, zh, "open_cv_failed", "Open CV Failed", "打开简历失败");
        put(en, zh, "desktop_open_not_supported", "Desktop open action is not supported on this platform.", "此平台不支持桌面打开操作。");
        put(en, zh, "unable_to_open_file", "Unable to open file", "无法打开文件");
        put(en, zh, "delete_cv_file", "Delete CV file", "删除简历文件");
        put(en, zh, "delete_cv_question", "Are you sure you want to delete your CV file?", "确定要删除您的简历文件吗？");
        put(en, zh, "delete_cv_failed_label", "Delete Failed", "删除失败");
        put(en, zh, "cv_deleted_label", "CV Deleted", "简历已删除");
        put(en, zh, "uploaded_cv_removed", "Uploaded CV has been removed.", "已上传的简历已被删除。");
        put(en, zh, "structured_cv", "Structured_CV.pdf", "结构化简历.pdf");
        put(en, zh, "cv_files", "CV files", "简历文件");
        put(en, zh, "pdf_files", "PDF files", "PDF 文件");
        put(en, zh, "docx_files", "DOCX files", "DOCX 文件");

        // ===================== APPLICANT PROFILE =====================
        put(en, zh, "personal_information", "Personal Information", "个人信息");
        put(en, zh, "edit_basic_information", "Edit Basic Information", "编辑基本信息");
        put(en, zh, "select_year", "Select year", "选择年级");
        put(en, zh, "select", "Select", "选择");
        put(en, zh, "pro_tip", "Pro Tip", "小贴士");
        put(en, zh, "complete_profile_unlock_apply", "Complete your profile to unlock all application features.", "完善您的个人资料以解锁所有申请功能。");
        put(en, zh, "profile_saved_label", "Profile Saved", "资料已保存");
        put(en, zh, "profile_saved_success", "Your profile has been saved successfully.", "您的资料已成功保存。");
        put(en, zh, "haidian_campus", "Haidian Campus", "海淀校区");
        put(en, zh, "shahe_campus_cvc", "Shahe Campus", "沙河校区");

        // ===================== JOB MANAGEMENT =====================
        put(en, zh, "my_jobs", "My Jobs", "我的职位");
        put(en, zh, "filter_btn", "Filter", "筛选");
        put(en, zh, "create_new_job", "+ Create New Job", "+ 创建新职位");
        put(en, zh, "job_title_header", "JOB TITLE", "职位名称");
        put(en, zh, "created_header", "CREATED", "创建时间");
        put(en, zh, "applicants_header", "APPLICANTS", "申请人");
        put(en, zh, "status_header", "STATUS", "状态");
        put(en, zh, "no_jobs_yet", "No job posts yet", "暂无职位发布");
        put(en, zh, "create_first_job", "Create your first job post to start collecting applications.", "创建您的第一个职位，开始收集申请。");
        put(en, zh, "select_a_job", "Select a job", "选择一个职位");
        put(en, zh, "choose_row_preview", "Choose a row to view job details and applicant activity.", "请选择一行以查看职位详情和申请人活动。");
        put(en, zh, "job_details_label", "JOB DETAILS", "职位详情");
        put(en, zh, "module_name_label", "Module name", "模块名称");
        put(en, zh, "module_code_label", "Module code", "模块代码");
        put(en, zh, "semester_label", "Semester", "学期");
        put(en, zh, "job_type_label", "Job type", "职位类型");
        put(en, zh, "open_slots_label", "Open slots", "开放名额");
        put(en, zh, "campus_label_mo", "Campus", "校区");
        put(en, zh, "job_id_label", "Job ID", "职位编号");
        put(en, zh, "publication_status", "Publication status", "发布状态");
        put(en, zh, "created_label", "Created", "创建时间");
        put(en, zh, "deadline_label_mo", "Deadline", "截止日期");
        put(en, zh, "applications_label", "Applications", "申请数");
        put(en, zh, "description_label", "DESCRIPTION", "描述");
        put(en, zh, "skills_requirements", "SKILLS & REQUIREMENTS", "技能与要求");
        put(en, zh, "required_skills", "Required Skills", "必需技能");
        put(en, zh, "preferred_skills", "Preferred Skills", "优先技能");
        put(en, zh, "min_grade", "Minimum Academic Grade", "最低学术成绩");
        put(en, zh, "applicant_status_label", "APPLICANT STATUS", "申请人状态");
        put(en, zh, "applied_label", "Applied", "已申请");
        put(en, zh, "under_review_label", "Under Review", "审核中");
        put(en, zh, "hired_label", "Hired", "已录用");
        put(en, zh, "no_applicants_yet", "No applicants yet", "暂无申请人");
        put(en, zh, "view_all_applicants", "View All Applicants", "查看全部申请人");
        put(en, zh, "edit_job_details", "Edit Job Details", "编辑职位详情");
        put(en, zh, "close_job_btn", "Close Job", "关闭职位");
        put(en, zh, "not_specified", "Not specified", "未指定");
        put(en, zh, "no_description_job", "No description provided for this job.", "此职位暂无描述。");
        put(en, zh, "select_job_first", "Please select one job first.", "请先选择一个职位。");
        put(en, zh, "close_job_confirm", "Close \"{title}\" now? Closed jobs cannot receive new applications.", "确定要关闭「{title}」吗？关闭后无法接收新申请。");
        put(en, zh, "job_closed", "Job Closed", "职位已关闭");
        put(en, zh, "job_closed_msg", "The job was set to CLOSED successfully.", "职位已成功关闭。");
        put(en, zh, "filter_jobs_title", "Filter Jobs", "筛选职位");
        put(en, zh, "filter_by_status", "Filter jobs by status", "按状态筛选职位");
        put(en, zh, "status_filter_label", "Status:", "状态：");

        // ===================== APPLICANT REVIEW =====================
        put(en, zh, "unable_to_load", "Unable to load application details.", "无法加载申请详情。");
        put(en, zh, "applicant_id_label", "Applicant ID: {id}", "申请人编号：{id}");
        put(en, zh, "basic_information", "Basic Information", "基本信息");
        put(en, zh, "applicant_statement", "Applicant Statement", "申请陈述");
        put(en, zh, "decision_note", "Decision Note", "决策备注");
        put(en, zh, "full_name_label", "FULL NAME", "姓名");
        put(en, zh, "student_id_label", "STUDENT ID", "学号");
        put(en, zh, "degree_program_label", "DEGREE PROGRAM", "学位项目");
        put(en, zh, "email_label", "EMAIL", "邮箱");
        put(en, zh, "cv_completion_label", "CV COMPLETION", "简历完成度");
        put(en, zh, "phone_label", "PHONE", "电话");
        put(en, zh, "campus_label_app", "CAMPUS", "校区");
        put(en, zh, "accept_cross_campus_label", "ACCEPT CROSS-CAMPUS", "接受跨校区");
        put(en, zh, "academic_year_label", "ACADEMIC YEAR", "学年");
        put(en, zh, "cv_complete_percent", "{n}% complete", "{n}% 完成");
        put(en, zh, "year_label", "Year {n}", "第 {n} 年");
        put(en, zh, "no_decision_note", "No decision note yet.", "暂无决策备注。");
        put(en, zh, "add_observation", "Add observation or justification for the recruitment decision...", "添加对招聘决策的观察或说明...");
        put(en, zh, "accept_candidate", "Accept Candidate", "接受候选人");
        put(en, zh, "reject_candidate", "Reject Candidate", "拒绝候选人");
        put(en, zh, "accept_confirm_msg", "Accept this applicant and update workload records?", "确定要接受此申请人并更新工作量记录吗？");
        put(en, zh, "reject_confirm_msg", "Reject this applicant for the selected job?", "确定要拒绝此申请人的职位申请吗？");
        put(en, zh, "applicant_cv_label", "APPLICANT CV", "申请人简历");
        put(en, zh, "no_cv_uploaded", "No CV uploaded", "未上传简历");
        put(en, zh, "no_cv_msg", "The applicant has not uploaded a CV file.", "此申请人尚未上传简历文件。");
        put(en, zh, "open_cv_system", "Open CV file in system viewer", "在系统查看器中打开简历");
        put(en, zh, "open_btn", "Open", "打开");
        put(en, zh, "operation_completed", "Operation completed.", "操作完成。");

        // ===================== JOB APPLY DIALOG =====================
        put(en, zh, "apply_for_job", "Apply for: {title}", "申请职位：{title}");
        put(en, zh, "modify_cv_hint", "If you want to modify the following content, please go to My CV to edit it.", "如需修改以下内容，请前往「我的简历」进行编辑。");
        put(en, zh, "name_label", "Name", "姓名");
        put(en, zh, "student_id_apply", "Student ID", "学号");
        put(en, zh, "email_apply", "Email address", "电子邮箱");
        put(en, zh, "phone_apply", "Phone number", "联系电话");
        put(en, zh, "major_apply", "Major", "专业");
        put(en, zh, "application_statement_title", "Application Statement", "申请陈述");
        put(en, zh, "explain_motivation", "Please explain your motivation and strengths for this role in English.", "请用英文说明您申请此职位的动机和优势。");
        put(en, zh, "describe_reasons", "Please describe your application reasons and strengths.", "请描述您的申请理由和优势。");
        put(en, zh, "preview_cv", "Preview CV", "预览简历");
        put(en, zh, "submit_btn", "Submit", "提交");
        put(en, zh, "cv_not_found", "CV Not Found", "未找到简历");
        put(en, zh, "no_uploaded_cv", "No uploaded CV file exists for this account.", "此账号不存在已上传的简历文件。");
        put(en, zh, "open_cv_failed", "Open CV Failed", "打开简历失败");
        put(en, zh, "desktop_open_unsupported", "Desktop open action is not supported on this platform.", "此平台不支持桌面打开操作。");
        put(en, zh, "unable_to_open_file", "Unable to open file: {error}", "无法打开文件：{error}");

        // ===================== DIALOG FACTORY =====================
        put(en, zh, "validation_error", "Validation Error", "验证错误");
        put(en, zh, "correct_input", "Please correct the input and try again.", "请修正输入后重试。");
        put(en, zh, "operation_failed", "Operation failed", "操作失败");
        put(en, zh, "operation_completed_dlg", "Operation completed", "操作完成");
        put(en, zh, "notification_title", "Notification", "通知");
        put(en, zh, "permission_denied", "Permission Denied", "权限不足");
        put(en, zh, "no_permission_action", "You do not have permission for this action.", "您没有执行此操作的权限。");
        put(en, zh, "workload_warn_title", "Workload Warning", "工作量警告");
        put(en, zh, "workload_warn_msg", "This action may exceed the policy limit.", "此操作可能超出政策上限。");
        put(en, zh, "confirm_action", "Please confirm this action.", "请确认此操作。");

        // ===================== HELP CENTER =====================
        put(en, zh, "help_login", "Login", "登录");
        put(en, zh, "help_login_desc", "- Enter your university username and password.\n- Use the eye button to show or hide the password.\n- Successful login routes you to the role-specific homepage.", "登录\n- 输入您的大学用户名和密码。\n- 使用眼睛按钮显示或隐藏密码。\n- 登录成功后跳转到对应角色的首页。");
        put(en, zh, "help_sample_accounts", "Sample accounts", "示例账号");
        put(en, zh, "help_ta_accounts", "- TA: ta001 / ta002 / ta003 / ta004 / ta005", "- TA账号：ta001 / ta002 / ta003 / ta004 / ta005");
        put(en, zh, "help_mo_accounts", "- MO: mo001 / mo002", "- MO账号：mo001 / mo002");
        put(en, zh, "help_admin_account", "- Admin: admin", "- 管理员：admin");
        put(en, zh, "help_password", "- Password: Password123!", "- 密码：Password123!");
        put(en, zh, "help_ta_features", "TA Features", "TA 功能");
        put(en, zh, "help_ta_browse", "- Browse available positions and view job details.", "- 浏览可用职位并查看职位详情。");
        put(en, zh, "help_ta_heart", "- Click the star icon to favourite jobs (displayed at top).", "- Click the star icon to favourite jobs (displayed at top).");
        put(en, zh, "help_ta_apply", "- Submit applications with a personal statement.", "- 提交申请并附上个人陈述。");
        put(en, zh, "help_ta_track", "- Track application status: Submitted → Under Review → Accepted/Rejected.", "- 跟踪申请状态：已提交 → 审核中 → 已接受/已拒绝。");
        put(en, zh, "help_ta_notifications", "- Receive notifications about application updates.", "- 接收申请状态更新的通知。");
        put(en, zh, "help_ta_profile", "- Manage your profile and CV in the Profile section.", "- 在个人资料部分管理您的资料和简历。");
        put(en, zh, "help_mo_features", "MO Features", "MO 功能");
        put(en, zh, "help_mo_manage", "- Create and manage job postings (Open/Closed).", "- 创建和管理职位发布（开放/关闭）。");
        put(en, zh, "help_mo_review", "- Review applicant applications and match scores.", "- 审核申请人的申请和匹配分数。");
        put(en, zh, "help_mo_decide", "- Make decisions: Accept or Reject applicants.", "- 做出决定：接受或拒绝申请人。");
        put(en, zh, "help_mo_workload", "- Monitor workload and export reports.", "- 监控工作量并导出报告。");
        put(en, zh, "help_mo_notify", "- Send notifications to applicants.", "- 向申请人发送通知。");
        put(en, zh, "help_admin_features", "Admin Features", "管理员功能");
        put(en, zh, "help_admin_users", "- Manage all users (TA, MO, Admin).", "- 管理所有用户（TA、MO、管理员）。");
        put(en, zh, "help_admin_stats", "- Monitor system-wide statistics.", "- 监控系统整体统计数据。");
        put(en, zh, "help_admin_export", "- Export workload and application reports.", "- 导出工作量及申请报告。");
        put(en, zh, "help_privacy", "Privacy Notice", "隐私声明");
        put(en, zh, "help_privacy_data", "- This system stores user, job, application, profile, and CV metadata locally in JSON files.", "- 本系统将用户、职位、申请、资料和简历元数据本地存储在 JSON 文件中。");
        put(en, zh, "help_privacy_hash", "- Passwords are stored as SHA-256 hashes, not as plain text.", "- 密码以 SHA-256 哈希值存储，而非明文。");
        put(en, zh, "help_privacy_cv", "- Uploaded CV files are used only for TA recruitment workflows.", "- 上传的简历文件仅用于 TA 招聘工作流程。");

        // ===================== ADMIN DASHBOARD =====================
        put(en, zh, "workload_monitoring", "Workload Monitoring", "工作量监控");
        put(en, zh, "search_ta_name_id", "Search TA name / ID", "搜索助教姓名 / 学号");
        put(en, zh, "all_risk", "All Risk", "全部风险");
        put(en, zh, "high_risk", "High", "高");
        put(en, zh, "medium_risk", "Medium", "中");
        put(en, zh, "low_risk", "Low", "低");
        put(en, zh, "export", "Export", "导出");
        put(en, zh, "total_jobs_kpi", "Total Jobs", "职位总数");
        put(en, zh, "total_applications_kpi", "Total Applications", "申请总数");
        put(en, zh, "accepted_apps_kpi", "Accepted Applications", "已录用申请");
        put(en, zh, "high_risk_tas_kpi", "High Risk TAs", "高风险助教");
        put(en, zh, "workload_monitoring_table", "Workload Monitoring", "工作量监控表");
        put(en, zh, "realtime_tracking_hours", "Real-time tracking of accepted jobs", "实时跟踪已接受岗位");
        put(en, zh, "filter", "Filter", "筛选");
        put(en, zh, "sort_by_risk", "Sort by Risk", "按风险排序");
        put(en, zh, "sort_by_hours", "Sort by Accepted Jobs", "按已接受岗位排序");
        put(en, zh, "ta_name", "TA Name", "助教姓名");
        put(en, zh, "accepted_jobs_label", "Accepted Jobs", "已接受职位");
        put(en, zh, "weekly_hours_label", "Accepted Jobs", "已接受岗位");
        put(en, zh, "max_limit_label", "Max Limit", "上限");
        put(en, zh, "risk_level_label", "Risk Level", "风险等级");
        put(en, zh, "notes_label", "Notes", "备注");
        put(en, zh, "no_workload_records_match", "No workload records match the filters.", "没有匹配的工作量记录。");
        put(en, zh, "showing_n_of_m_tas", "Showing {n} of {m} TAs", "显示 {n} / {m} 位助教");
        put(en, zh, "audit_log", "Audit Log", "审计日志");
        put(en, zh, "audit_log_desc", "Recent actions across the recruitment system", "招聘系统最近的操作记录");
        put(en, zh, "time_label", "Time", "时间");
        put(en, zh, "actor_label", "Actor", "操作人");
        put(en, zh, "action_label2", "Action", "操作");
        put(en, zh, "detail_label", "Detail", "详情");
        put(en, zh, "no_audit_logs_found", "No audit logs found.", "未找到审计日志。");
        put(en, zh, "workload_csv_generated", "Workload report generated", "工作量报告已生成");
        put(en, zh, "exported_colon", "Exported:", "已导出：");

        // ===================== ADMIN JOBS =====================
        put(en, zh, "jobs_admin", "Jobs", "职位");
        put(en, zh, "search_title_id_module", "Search title / ID / module", "搜索标题 / 编号 / 模块");
        put(en, zh, "all_status", "All Status", "全部状态");
        put(en, zh, "open", "OPEN", "开放");
        put(en, zh, "closed", "CLOSED", "已关闭");
        put(en, zh, "expired", "EXPIRED", "已过期");
        put(en, zh, "draft", "DRAFT", "草稿");
        put(en, zh, "all_types", "All Types", "全部类型");
        put(en, zh, "module_ta", "Module TA", "模块助教");
        put(en, zh, "invigilation", "Invigilation", "监考");
        put(en, zh, "activity_support", "Activity Support", "活动支持");
        put(en, zh, "other", "Other", "其他");
        put(en, zh, "edit_job", "Edit Job", "编辑职位");
        put(en, zh, "close_job", "Close Job", "关闭职位");
        put(en, zh, "total_openings", "Total Openings", "总开放数");
        put(en, zh, "total_applicants_admin", "Total Applicants", "总申请人");
        put(en, zh, "active_jobs", "Active Jobs", "活跃职位");
        put(en, zh, "job_title_id", "Job Title / ID", "职位 / 编号");
        put(en, zh, "department_admin", "Department", "部门");
        put(en, zh, "applicants_count", "Applicants", "申请人");
        put(en, zh, "status_upper", "STATUS", "状态");
        put(en, zh, "detail_upper", "Detail", "详情");
        put(en, zh, "no_jobs_match_filters", "No jobs match the filters.", "没有匹配的职位。");
        put(en, zh, "no_recent_activity", "No recent activity", "暂无最近活动");
        put(en, zh, "job_details_admin", "Job Details", "职位详情");
        put(en, zh, "no_applicants_yet_job", "No applicants yet", "暂无申请人");
        put(en, zh, "no_mo_available", "No MO accounts available.", "暂无可用的 MO 账号。");
        put(en, zh, "please_select_job_first", "Please select a job first.", "请先选择一个职位。");
        put(en, zh, "job_could_not_be_loaded", "Job could not be loaded.", "无法加载职位。");
        put(en, zh, "close_job_question", "Close this job?", "确定关闭此职位？");
        put(en, zh, "job_closed_desc", "The job has been closed.", "职位已关闭。");
        put(en, zh, "applications_for_job", "Applications for job", "职位申请");
        put(en, zh, "job_details_upper", "JOB DETAILS", "职位详情");
        put(en, zh, "associated_module", "Associated Module", "关联模块");
        put(en, zh, "job_summary", "Job Summary", "职位概要");
        put(en, zh, "weekly_hours_summary", "Load", "负载");
        put(en, zh, "positions_summary", "Positions", "名额");
        put(en, zh, "deadline_summary", "Deadline", "截止日期");
        put(en, zh, "created_summary", "Created", "创建时间");
        put(en, zh, "required_skills_summary", "Required Skills", "必需技能");
        put(en, zh, "preferred_skills_summary", "Preferred Skills", "优先技能");
        put(en, zh, "applicant_status_upper", "Applicant Status", "申请人状态");
        put(en, zh, "applied_count", "Applied", "已申请");
        put(en, zh, "in_review_count", "In Review", "审核中");
        put(en, zh, "hired_count", "Hired", "已录用");
        put(en, zh, "activity_log", "Activity Log", "活动日志");
        put(en, zh, "view_all_applications", "View All Applications", "查看全部申请");

        // ===================== ADMIN APPLICATIONS =====================
        put(en, zh, "applications_admin", "Applications", "申请管理");
        put(en, zh, "export_applications", "Export Applications", "导出申请");
        put(en, zh, "search_applicant_id_job", "Search applicant / ID / job", "搜索申请人 / 学号 / 职位");
        put(en, zh, "submitted_upper2", "SUBMITTED", "已提交");
        put(en, zh, "under_review_upper", "UNDER REVIEW", "审核中");
        put(en, zh, "accepted_upper", "ACCEPTED", "已接受");
        put(en, zh, "rejected_upper", "REJECTED", "已拒绝");
        put(en, zh, "application_queue", "Application Queue", "申请队列");
        put(en, zh, "filtered_list_candidates", "Filtered list of candidates", "已筛选的候选人列表");
        put(en, zh, "applicant_upper", "Applicant", "申请人");
        put(en, zh, "match_upper", "Match", "匹配度");
        put(en, zh, "risk_upper", "Risk", "风险");
        put(en, zh, "no_applications_match_filters", "No applications match the filters.", "没有匹配的申请。");
        put(en, zh, "select_an_application", "Select an application", "请选择一个申请");
        put(en, zh, "no_skills_loaded", "No skills loaded", "未加载技能");
        put(en, zh, "no_availability_loaded", "No availability loaded", "未加载可用时间");
        put(en, zh, "no_missing_skills_upper", "No missing skills", "无缺失技能");
        put(en, zh, "not_provided", "Not provided", "未提供");
        put(en, zh, "no_data", "No data", "无数据");
        put(en, zh, "please_select_application_first", "Please select an application first.", "请先选择一个申请。");
        put(en, zh, "applicant_review_admin", "Applicant Review", "申请人审核");
        put(en, zh, "accept_candidate_question", "Accept this candidate?", "确定接受此候选人？");
        put(en, zh, "reject_candidate_question", "Reject this candidate?", "确定拒绝此候选人？");
        put(en, zh, "application_csv_generated", "Application report generated", "申请报告已生成");
        put(en, zh, "application_detail_admin", "Application Detail", "申请详情");
        put(en, zh, "skills_competencies", "Skills & Competencies", "技能与能力");
        put(en, zh, "availability_upper", "Availability", "可用时间");
        put(en, zh, "missing_skills_upper", "Missing Skills", "缺失技能");
        put(en, zh, "attachments_upper", "Attachments", "附件");
        put(en, zh, "candidate_statement", "Candidate Statement", "候选人陈述");
        put(en, zh, "open_review_workspace", "Open Review Workspace", "打开审核工作区");
        put(en, zh, "add_observations_justification", "Add observations or justification...", "添加观察或说明...");
        put(en, zh, "match_score", "Match Score", "匹配分数");
        put(en, zh, "workload_label", "Risk Assessment", "风险评估");
        put(en, zh, "workload_check", "Risk Assessment", "风险评估");
        put(en, zh, "load_label", "Load", "负载");
        put(en, zh, "sort_by_accepted_jobs", "Sort by Accepted Jobs", "按已接受岗位排序");
        put(en, zh, "missing_skills_label", "Missing Skills", "缺失技能");
        put(en, zh, "statement_upper", "Statement", "陈述");
        put(en, zh, "degree_program", "Degree / Programme", "学位 / 专业");
        put(en, zh, "accepted_candidate_chip", "Accepted", "已接受");
        put(en, zh, "rejected_candidate_chip", "Rejected", "已拒绝");
        put(en, zh, "active_candidate_chip", "Active", "进行中");

        // ===================== ADMIN NOTIFICATIONS =====================
        put(en, zh, "risk_change_title", "TA Workload Risk Changed", "助教工作量风险变化");
        put(en, zh, "risk_change_message", "{name} risk level changed from {from} to {to}.", "{name} 的风险等级从 {from} 变为 {to}。");

        translations.put(EN, en);
        translations.put(ZH, zh);
    }

    private static void put(Map<String, String> en, Map<String, String> zh, String key, String enVal, String zhVal) {
        en.put(key, enVal);
        zh.put(key, zhVal);
    }
}
