package com.example.demo.web;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

//import com.example.demo.service.InMemoryPaperService;
import com.example.demo.service.PaperService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Papers", description = "論文の作成・取得・検索・更新・削除")
@RestController
@RequestMapping("/api/papers")
public class PaperController {
	
//	private final InMemoryPaperService svc;
	private final PaperService svc;
//	public PaperController(InMemoryPaperService svc) {
	public PaperController(PaperService svc) {
		this.svc = svc;
	}
	
	public static record PaperCreateReq(
		  @Schema(description="タイトル", example="QUIC Survey") @NotBlank String title,
		  @Schema(description="著者", example="Yan et al.") @Size(max=800) String authors,
		  @Schema(description="出版年", example="2021", minimum="1900", maximum="2100") @Min(1900) @Max(2100) Integer year,
		  @Schema(description="URL", example="https://example.com/paper") String url
	) {}
	
	public static record PaperRes(
			Long id,
			String title,
			String authors,
			Integer year,
			String url,
			long createdAt,
			String status,
			List<String> tags
	) {}
	
	private static PaperRes toRes(com.example.demo.service.PaperService.PaperRow e) {
	    return new PaperRes(e.id(), e.title(), e.authors(), e.year(), e.url(),
	                        e.createdAt(), e.status(), e.tags()); // ← ここで tags を渡す
	}
	
	@Operation(summary = "論文を作成", description = "title は必須。year は 1900〜2100。")
	@ApiResponses({
	  @ApiResponse(responseCode = "201", description = "作成成功"),
	  @ApiResponse(responseCode = "400", description = "入力バリデーションNG")
	})
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public PaperRes create(@RequestBody @Valid PaperCreateReq req) {
		var e = svc.create(req.title(), req.authors(), req.year(), req.url());
		return toRes(e);
	}
	
	@GetMapping("/{id}")
	public PaperRes get(@PathVariable Long id) {
		var e = svc.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Paper not found: " + id));
		return toRes(e);
	}
	
	
	public static record PageRes<T>(List<T> content, long total, int page, int size, boolean hasNext){}
	
	@Operation(summary = "論文一覧", description = "キーワード(q)とstatusで検索。ページング対応。")
	@GetMapping
	public PageRes<PaperRes> list(@Parameter(description="0始まりのページ番号", example="0") @RequestParam(defaultValue="0") @Min(0) int page,
								  @Parameter(description="ページサイズ(1-200)", example="10") @RequestParam(defaultValue="10") @Min(1) @Max(200) int size,
								  @Parameter(description="キーワード（title/authors 部分一致）", example="quic") @RequestParam(required=false) String q,
								  @Parameter(description="UNREAD/READING/DONE", example="UNREAD") @RequestParam(required=false) String status,
								  @RequestParam(required=false) List<String> tags
			){
		var rows = svc.findAll(page, size, q, status, tags).stream()
				.map(e -> toRes(e))
				.toList();
		long total = svc.countFiltered(q, status, tags);
		boolean hasNext = (long)(page + 1) * size < total;
		return new PageRes<>(rows, total, page, size, hasNext);
	}
	
	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable Long id) {
		if(svc.findById(id).isEmpty())
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Paper not found: " + id);
		svc.delete(id);
	}
	
	public static record PaperUpdateReq(String title, String authors, Integer year, String url) {}
	
	@PatchMapping("/{id}")
	public PaperRes update(@PathVariable Long id, @RequestBody PaperUpdateReq req) {
		var updated = svc.update(id, req.title(), req.authors(), req.year(), req.url())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Paper not found: " + id));
		return toRes(updated);
	}
	
	public static record StatusReq(String status) {}
	
	@PatchMapping("/{id}/status")
	public PaperRes updateStatus(@PathVariable Long id, @RequestBody @Valid StatusReq req) {
		if (req.status() == null || req.status().isBlank()) {
			throw new org.springframework.web.server.ResponseStatusException(
					org.springframework.http.HttpStatus.BAD_REQUEST, "status is required");
		}
		var wanted = req.status().trim().toUpperCase();
		
	    var allowed = java.util.Set.of("UNREAD", "READING", "DONE");
	    if (!allowed.contains(wanted)) {
	        throw new org.springframework.web.server.ResponseStatusException(
	                org.springframework.http.HttpStatus.BAD_REQUEST, "status must be one of " + allowed);
	    }
	    
	    var updated = svc.setStatus(id, wanted)
	    		.orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND, "Paper not found: " + id));
	    
	    return toRes(updated);
	}
	
	public static record TagReq(String tag) {}

	@PostMapping("/{id}/tags")
	public PaperRes addTag(@PathVariable Long id, @RequestBody @Valid TagReq req) {
	  var updated = svc.addTag(id, req.tag())
	      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Paper not found: " + id));
	  return toRes(updated);
	}

	@DeleteMapping("/{id}/tags/{tag}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void removeTag(@PathVariable Long id, @PathVariable String tag) {
	  if (svc.findById(id).isEmpty())
	    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Paper not found: " + id);
	  svc.removeTag(id, tag);
	}

}




















