package Group4.Childcare.Service;

import Group4.Childcare.Model.Users;
import Group4.Childcare.Repository.UserJdbcRepository;
import Group4.Childcare.DTO.UserSummaryDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UsersService {
    @Autowired
    private UserJdbcRepository repository;

    /**
     * 新增使用者
     * @param user Users 實體
     * @return 新增後的 Users
     */
    public Users createUser(Users user) {
        return repository.save(user);
    }

    /**
     * 依使用者ID查詢使用者
     * @param id 使用者ID
     * @return 查詢結果 Optional<Users>
     */
    public Optional<Users> getUserById(UUID id) {
        return repository.findById(id);
    }

    /**
     * 查詢所有使用者
     * @return 使用者列表
     */
    public List<Users> getAllUsers() {
        return repository.findAll();
    }

    /**
     * 更新使用者資料
     * @param id 使用者ID
     * @param user 更新內容
     * @return 更新後的 Users
     */
    public Users updateUser(UUID id, Users user) {
        user.setUserID(id);
        return repository.save(user);
    }

    // 使用JDBC的offset分頁方法，包含機構名稱 - 可指定每頁筆數
    public List<UserSummaryDTO> getUsersWithOffsetAndInstitutionNameJdbc(int offset, int size) {
        try {
            return repository.findWithOffsetAndInstitutionName(offset, size);
        } catch (Exception e) {
            System.err.println("Error in getUsersWithOffsetAndInstitutionNameJdbc: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to get users with offset", e);
        }
    }

    // 取得總筆數用於分頁計算
    public long getTotalCount() {
        try {
            return repository.countTotal();
        } catch (Exception e) {
            System.err.println("Error in getTotalCount: " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 使用 UserJdbcRepository 的 save 方法（支援 insert 或 update）
     * 若 userID 為 null 則自動產生 UUID
     * 若 familyInfoID 為 null 則自動建立新的 FamilyInfo
     * @param user Users 實體（必須包含 account、password；name 和 nationalID 可為 null）
     * @return 儲存後的 Users
     * @throws RuntimeException 若保存失敗
     */
    public Users saveUsingJdbc(Users user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        try {
            Users saved = repository.save(user);
            System.out.println("User saved successfully: " + saved.getUserID());
            return saved;
        } catch (Exception e) {
            System.err.println("Error in saveUsingJdbc: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("無法註冊: " + e.getMessage(), e);
        }
    }

    /**
     * 安全更新 accountStatus（僅更新帳號狀態欄位）
     * @param id 使用者 ID
     * @param accountStatus 要設定的狀態（0 或 1）
     * @return 更新後的 Users
     */
    public Users updateAccountStatus(UUID id, Integer accountStatus) {
        // 使用 JDBC 部分更新 accountStatus
        try {
            int rows = repository.updateAccountStatus(id, accountStatus);
            if (rows == 0) {
                throw new RuntimeException("User not found or no change: " + id);
            }
            // 透過 JDBC 重新讀取最新的 user 資料
            Optional<Users> updated = repository.findById(id);
            if (updated.isPresent()) {
                return updated.get();
            } else {
                // 這種情況不太可能發生，但提供回退
                throw new RuntimeException("Failed to read updated user: " + id);
            }
        } catch (Exception e) {
            System.err.println("Error in updateAccountStatus via JDBC: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to update account status via JDBC", e);
        }
    }

    /**
     * 使用 JDBC 部分更新使用者基本資料（僅更新姓名、信箱、電話、地址）
     * @param id 使用者ID
     * @param name 姓名（可為 null，表示不更新）
     * @param email 信箱（可為 null，表示不更新）
     * @param phoneNumber 電話（可為 null，表示不更新）
     * @param mailingAddress 地址（可為 null，表示不更新）
     * @return 更新的行數
     */
    public int updateUserProfile(UUID id, String name, String email, String phoneNumber, String mailingAddress) {
        try {
            return repository.updateProfile(id, name, email, phoneNumber, mailingAddress);
        } catch (Exception e) {
            System.err.println("Error in updateUserProfile: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to update user profile: " + e.getMessage(), e);
        }
    }

    /**
     * 模糊查詢使用者，支援分頁
     * @param searchTerm 搜尋關鍵字（會搜尋帳號、姓名、信箱、機構名稱）
     * @param offset 起始位置
     * @param size 分頁大小
     * @return 符合條件的使用者列表
     */
    public List<UserSummaryDTO> searchUsersWithOffset(String searchTerm, int offset, int size) {
        try {
            if (searchTerm == null || searchTerm.trim().isEmpty()) {
                // 如果搜尋關鍵字為空，返回一般分頁結果
                return getUsersWithOffsetAndInstitutionNameJdbc(offset, size);
            }
            return repository.searchUsersWithOffset(searchTerm.trim(), offset, size);
        } catch (Exception e) {
            System.err.println("Error in searchUsersWithOffset: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to search users with offset", e);
        }
    }

    /**
     * 計算模糊查詢的總筆數
     * @param searchTerm 搜尋關鍵字
     * @return 符合條件的總筆數
     */
    public long getSearchCount(String searchTerm) {
        try {
            if (searchTerm == null || searchTerm.trim().isEmpty()) {
                return getTotalCount();
            }
            return repository.countSearchUsers(searchTerm.trim());
        } catch (Exception e) {
            System.err.println("Error in getSearchCount: " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
        }

    /**
     * 檢查帳號是否已存在
     * @param account 帳號
     * @return true 表示帳號已存在，false 表示帳號可用
     */
    public boolean isAccountExists(String account) {
        try {
            if (account == null || account.trim().isEmpty()) {
                return false;
            }
            Optional<Users> user = repository.findByAccount(account.trim());
            return user.isPresent();
        } catch (Exception e) {
            System.err.println("Error in isAccountExists: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 檢查電子信箱是否已存在
     * @param email 電子信箱
     * @return true 表示信箱已存在，false 表示信箱可用
     */
    public boolean isEmailExists(String email) {
        try {
            if (email == null || email.trim().isEmpty()) {
                return false;
            }
            Optional<Users> user = repository.findByEmail(email.trim());
            return user.isPresent();
        } catch (Exception e) {
            System.err.println("Error in isEmailExists: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
