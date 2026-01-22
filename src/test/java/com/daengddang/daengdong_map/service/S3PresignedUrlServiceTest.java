package com.daengddang.daengdong_map.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.daengddang.daengdong_map.common.ErrorCode;
import com.daengddang.daengdong_map.common.exception.BaseException;
import com.daengddang.daengdong_map.domain.s3.FileType;
import com.daengddang.daengdong_map.domain.s3.UploadContext;
import com.daengddang.daengdong_map.domain.user.User;
import com.daengddang.daengdong_map.dto.request.s3.PresignedUrlRequest;
import com.daengddang.daengdong_map.dto.response.s3.PresignedUrlResponse;
import com.daengddang.daengdong_map.repository.UserRepository;
import java.net.URL;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

@ExtendWith(MockitoExtension.class)
class S3PresignedUrlServiceTest {

    @Mock
    private S3Presigner s3Presigner;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private S3PresignedUrlService s3PresignedUrlService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(s3PresignedUrlService, "bucket", "test-bucket");
    }

    @Test
    void issuePresignedUrl_returnsResponse_whenValid() throws Exception {
        PresignedUrlRequest request = buildRequest(
                FileType.IMAGE,
                "image/jpeg",
                UploadContext.WALK
        );

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(User.builder().kakaoUserId(1L).build()));

        PresignedPutObjectRequest presigned = org.mockito.Mockito.mock(PresignedPutObjectRequest.class);
        when(presigned.url()).thenReturn(new URL("https://s3.ap-northeast-2.amazonaws.com/test"));
        when(s3Presigner.presignPutObject(any())).thenReturn(presigned);

        PresignedUrlResponse response =
                s3PresignedUrlService.issuePresignedUrl(1L, request);

        assertThat(response.getPresignedUrl())
                .isEqualTo("https://s3.ap-northeast-2.amazonaws.com/test");
        assertThat(response.getExpiresIn()).isEqualTo(300);
        assertThat(response.getObjectKey()).startsWith("walk/");
        assertThat(response.getObjectKey()).contains("/" + java.time.LocalDate.now().toString().replace("-", "/") + "/");
    }

    @Test
    void issuePresignedUrl_throws_whenUserNotFound() {
        PresignedUrlRequest request = buildRequest(
                FileType.IMAGE,
                "image/jpeg",
                UploadContext.WALK
        );

        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> s3PresignedUrlService.issuePresignedUrl(1L, request))
                .isInstanceOf(BaseException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    @Test
    void issuePresignedUrl_throws_whenContentTypeMismatch() {
        PresignedUrlRequest request = buildRequest(
                FileType.IMAGE,
                "video/mp4",
                UploadContext.WALK
        );

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(User.builder().kakaoUserId(1L).build()));

        assertThatThrownBy(() -> s3PresignedUrlService.issuePresignedUrl(1L, request))
                .isInstanceOf(BaseException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FILE_TYPE_UNSUPPORTED);
    }

    @Test
    void issuePresignedUrl_throws_whenExtensionUnsupported() {
        PresignedUrlRequest request = buildRequest(
                FileType.IMAGE,
                "image/tiff",
                UploadContext.WALK
        );

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(User.builder().kakaoUserId(1L).build()));

        assertThatThrownBy(() -> s3PresignedUrlService.issuePresignedUrl(1L, request))
                .isInstanceOf(BaseException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FILE_TYPE_UNSUPPORTED);
    }

    private PresignedUrlRequest buildRequest(FileType fileType, String contentType, UploadContext uploadContext) {
        PresignedUrlRequest request = new PresignedUrlRequest();
        ReflectionTestUtils.setField(request, "fileType", fileType);
        ReflectionTestUtils.setField(request, "contentType", contentType);
        ReflectionTestUtils.setField(request, "uploadContext", uploadContext);
        return request;
    }
}
