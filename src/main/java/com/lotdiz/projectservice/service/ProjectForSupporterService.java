package com.lotdiz.projectservice.service;

import com.lotdiz.projectservice.client.FundingServiceClient;
import com.lotdiz.projectservice.client.MemberServiceClient;
import com.lotdiz.projectservice.dto.ProductDto;
import com.lotdiz.projectservice.dto.ProjectImageDto;
import com.lotdiz.projectservice.dto.response.FundingAchievementResultOfProjectDetailResponseDto;
import com.lotdiz.projectservice.dto.response.FundingAchievementResultOfProjectResponseDto;
import com.lotdiz.projectservice.dto.response.ProjectByCategoryResponseDto;
import com.lotdiz.projectservice.dto.response.ProjectDetailResponseDto;
import com.lotdiz.projectservice.entity.Project;
import com.lotdiz.projectservice.exception.FundingServiceClientOutOfServiceException;
import com.lotdiz.projectservice.exception.MemberServiceClientOutOfServiceException;
import com.lotdiz.projectservice.exception.ProjectEntityNotFoundException;
import com.lotdiz.projectservice.repository.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProjectForSupporterService {

  private final ProjectRepository projectRepository;
  private final LotdealRepository lotdealRepository;
  private final ProjectImageRepository projectImageRepository;
  private final ProductRepository productRepository;
  private final SupportSignatureRepository supportSignatureRepository;
  private final FundingServiceClient fundingServiceClient;
  private final MemberServiceClient memberServiceClient;
  private final CircuitBreakerFactory circuitBreakerFactory;

  @Transactional(readOnly = true)
  public List<ProjectByCategoryResponseDto> getProjectsByCategory(
      String categoryName, Pageable pageable, Long memberId) {

    CircuitBreaker circuitBreaker = circuitBreakerFactory.create("circuitBreaker");

    List<ProjectByCategoryResponseDto> projectByCategoryResponseDtoList = new ArrayList<>();
    List<Long> projectIds = new ArrayList<>();

    Page<Project> projects =
        projectRepository.findByCategoryAndProjectIsAuthorized(categoryName, true, pageable);

    projects.forEach(p -> projectIds.add(p.getProjectId()));

    Map<String, Boolean> likedProjects =
        (Map<String, Boolean>)
            circuitBreaker.run(
                () -> memberServiceClient.getIsLike(memberId, projectIds).getData(),
                throwable -> new MemberServiceClientOutOfServiceException());

    HashMap<String, FundingAchievementResultOfProjectResponseDto>
        fundingAchievementResultOfProjectResponseDtoList =
            (HashMap<String, FundingAchievementResultOfProjectResponseDto>)
                circuitBreaker.run(
                    () -> fundingServiceClient.getFundingOfProject(projectIds).getData(),
                    throwable -> new FundingServiceClientOutOfServiceException());

    for (Project p : projects) {
      ProjectByCategoryResponseDto projectByCategoryResponseDto =
          lotdealRepository
              .findByProjectAndLotdealing(p, LocalDateTime.now())
              .map(
                  lotdeal ->
                      ProjectByCategoryResponseDto.fromProjectEntity(
                          p,
                          likedProjects.get(Long.toString(p.getProjectId())),
                          fundingAchievementResultOfProjectResponseDtoList.get(
                              Long.toString(p.getProjectId())),
                          lotdeal.getLotdealDueTime()))
              .orElseGet(
                  () ->
                      ProjectByCategoryResponseDto.fromProjectEntity(
                          p,
                          likedProjects.get(Long.toString(p.getProjectId())),
                          fundingAchievementResultOfProjectResponseDtoList.get(
                              Long.toString(p.getProjectId())),
                          null));

      projectByCategoryResponseDtoList.add(projectByCategoryResponseDto);
    }
    return projectByCategoryResponseDtoList;
  }

  @Transactional(readOnly = true)
  public ProjectDetailResponseDto getProjectDetails(Long projectId) {

    CircuitBreaker circuitBreaker = circuitBreakerFactory.create("circuitBreaker");

    Project project =
        projectRepository.findById(projectId).orElseThrow(ProjectEntityNotFoundException::new);

    List<ProjectImageDto> projectImageDtoList =
        projectImageRepository.findByProject(project).stream()
            .map(projectImage -> ProjectImageDto.fromProjectImageEntity(projectImage))
            .collect(Collectors.toList());

    List<ProductDto> productDtoList =
        productRepository.findByProject(project).stream()
            .map(product -> ProductDto.fromProductEntity(product))
            .collect(Collectors.toList());

    Long numberOfSupporter = supportSignatureRepository.countByProject(project);

    Long likeCount =
            (Long)
                    circuitBreaker.run(
                            () -> memberServiceClient.getLikeCount(projectId).getData().get(Long.toString(projectId)),
                            throwable -> new MemberServiceClientOutOfServiceException());

    FundingAchievementResultOfProjectDetailResponseDto
        fundingAchievementResultOfProjectDetailResponseDto =
            (FundingAchievementResultOfProjectDetailResponseDto)
                circuitBreaker.run(
                    () -> fundingServiceClient.getFundingOfProjectDetail(projectId).getData(),
                    throwable -> new FundingServiceClientOutOfServiceException());

    ProjectDetailResponseDto projectDetailResponseDto =
        lotdealRepository
            .findByProjectAndLotdealing(project, LocalDateTime.now())
            .map(
                lotdeal ->
                    ProjectDetailResponseDto.fromProjectEntity(
                        project,
                        projectImageDtoList,
                        productDtoList,
                        likeCount,
                        fundingAchievementResultOfProjectDetailResponseDto,
                        numberOfSupporter,
                        lotdeal.getLotdealDueTime()))
            .orElseGet(
                () ->
                    ProjectDetailResponseDto.fromProjectEntity(
                        project,
                        projectImageDtoList,
                        productDtoList,
                        likeCount,
                        fundingAchievementResultOfProjectDetailResponseDto,
                        numberOfSupporter,
                        null));

    return projectDetailResponseDto;
  }
}
