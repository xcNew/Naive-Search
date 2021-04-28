//package com.txc.search.repository.mysql;
//
//import com.txc.search.entity.mysql.MysqlSearchEntity;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.data.repository.query.Param;
//
//import java.util.List;
//
///**
// * @author tianxiaochen
// */
//public interface MysqlSearchRepository extends JpaRepository<MysqlSearchEntity, Integer> {
//    /**
//     * 创建时间倒序查询
//     *
//     * @return
//     */
//    @Query("select e from MysqlSearchEntity e order by e.createTime desc ")
//    List<MysqlSearchEntity> queryAll();
//
//    /**
//     * 模糊查询
//     *
//     * @param keyword
//     * @return
//     */
//    @Query("select e from MysqlSearchEntity e where e.title like concat('%',:keyword,'%') or e.content like concat('%',:keyword,'%')")
//    List<MysqlSearchEntity> queryEntities(@Param("keyword") String keyword);
//
//}
