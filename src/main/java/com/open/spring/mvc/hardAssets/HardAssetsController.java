package com.open.spring.mvc.hardAssets;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/assets")
public class HardAssetsController {

    private final HardAssetsRepository repository;
    private final HardAssetUploadService uploadService;

    public HardAssetsController(HardAssetsRepository repository, HardAssetUploadService uploadService) {
        this.repository = repository;
        this.uploadService = uploadService;
    }

    @GetMapping("/upload/{id}")
    public ResponseEntity<HardAsset> getAsset(@PathVariable Long id) {
        Optional<HardAsset> optional = repository.findById(id);
        if (optional.isPresent()) { // Good ID
            HardAsset asset = optional.get(); // value from findByID
            return new ResponseEntity<>(asset, HttpStatus.OK); // OK HTTP response: status code, headers, and body
        }
        // Bad ID
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @GetMapping("/uploads")
    public ResponseEntity<List<HardAsset>> getUploads(@RequestParam(required = false) String uid, @AuthenticationPrincipal UserDetails userDetails) {
        String targetUid = uid;
        if (targetUid == null) {
            if (userDetails == null) {
                return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
            }
            targetUid = userDetails.getUsername();
        }
        
        List<HardAsset> assets = repository.findByOwnerUID(targetUid);
        return new ResponseEntity<>(assets, HttpStatus.OK);
    }

    @PostMapping("/upload")
    public ResponseEntity<String> postUpload(@RequestParam("file") MultipartFile file, @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return new ResponseEntity<>("User not authenticated.", HttpStatus.UNAUTHORIZED);
        }

        try {
            String uid = userDetails.getUsername();
            HardAssetUploadService.UploadResult result = uploadService.upload(file, uid);

            HardAsset newAsset = new HardAsset(result.getOriginalFilename(), result.getLocalFileUUID(), result.getUid());
            repository.save(newAsset);
            return new ResponseEntity<>("Successfully uploaded and saved '" + result.getLocalFileUUID() + "'", HttpStatus.OK);
        } catch (HardAssetUploadService.UploadException e) {
            return new ResponseEntity<>(e.getMessage(), e.getStatus());
        }
    }
}