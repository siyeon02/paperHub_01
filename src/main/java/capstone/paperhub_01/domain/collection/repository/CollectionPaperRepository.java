package capstone.paperhub_01.domain.collection.repository;

import capstone.paperhub_01.domain.collection.CollectionPaper;
import capstone.paperhub_01.domain.collection.ReadingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CollectionPaperRepository extends JpaRepository<CollectionPaper, Long> {
  Optional<CollectionPaper> findByPaperId(Long paperId);

  // @Query("""
  // select cp from CollectionPaper cp
  // join fetch cp.paper p
  // where cp.member.id = :memberId
  // and cp.status = :status
  // """)
  // Page<CollectionPaper> searchByMemberAndStatus(@Param("memberId") Long
  // memberId,
  // @Param("status") ReadingStatus status,
  // Pageable pageable);

  @Query(value = """
        select cp
        from CollectionPaper cp
          join fetch cp.paper p
          left join fetch p.paperInfo i
        where cp.member.id = :memberId
          and (:rs is null or cp.status = :rs)
        order by cp.updatedAt desc
      """, countQuery = """
        select count(cp)
        from CollectionPaper cp
        where cp.member.id = :memberId
          and (:rs is null or cp.status = :rs)
      """)
  Page<CollectionPaper> searchByMemberAndStatus(@Param("memberId") Long memberId,
      @Param("rs") ReadingStatus rs,
      Pageable pageable);

  @Query("""
          select cp
          from CollectionPaper cp
            join fetch cp.paper p
            left join fetch p.paperInfo i
          where cp.member.id = :memberId
            and (:rs is null or cp.status = :rs)
          order by cp.updatedAt desc
      """)
  List<CollectionPaper> findAllByMemberAndStatus(@Param("memberId") Long memberId,
      @Param("rs") ReadingStatus rs);

  @Query("""
        select cp
        from CollectionPaper cp
          left join fetch cp.paper p
          left join fetch p.paperInfo i
        where cp.id = :id
          and cp.member.id = :memberId
      """)
  Optional<CollectionPaper> findInfoByIdAndMember(@Param("id") Long id,
      @Param("memberId") Long memberId);

  // @Query("""
  // select cp from CollectionPaper cp
  // join fetch cp.paper p
  // where cp.id = :id
  // and cp.member.id = :memberId
  // """)
  // Optional<CollectionPaper> findInfoByIdAndMember(Long id, Long memberId);

  @Query("""
          select cp.status as status, count(cp) as count
          from CollectionPaper cp
          where cp.member.id = :memberId
          group by cp.status
      """)
  List<StatusCountProjection> countByMemberGrouped(@Param("memberId") Long memberId);

  interface StatusCountProjection {
    ReadingStatus getStatus();

    long getCount();
  }
}
