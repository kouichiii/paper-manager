//package com.example.demo.web;
//
//import jakarta.validation.Valid;
//
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PathVariable;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.bind.annotation.RestController;
//
//import com.example.demo.service.InMemoryPaperService;
//
//@RestController
//public class HelloController {
//	
//	private final InMemoryPaperService svc;
//	public HelloController(InMemoryPaperService svc) {  // ← ここでDI
//        this.svc = svc;
//    }
//
//    @GetMapping("/api/_probe/create")
//    public String probeCreate() {                        // ← 引数から外す
//        var e = svc.create("Probe Paper", "Tester", 2025, "http://example.com");
//        return "created id=" + e.id();
//    }
//
//    @GetMapping("/api/_probe/count")
//    public String probeCount() {                         // ← 引数から外す
//        return "count=" + svc.count();
//    }
//	
//	record PaperCreateReq(
//			@jakarta.validation.constraints.NotBlank
//			String title,
//			@jakarta.validation.constraints.Size(max = 800)
//			String authors,
//			@jakarta.validation.constraints.Min(1900)
//			@jakarta.validation.constraints.Max(2100)
//			Integer year,
//			String url
//	) {}
//	
//	record PaperCreateRes(
//			String title,
//			String authors,
//			Integer year,
//			String url,
//			long createdAt
//	) {}
//	
//	@PostMapping("/api/papers")
//	public PaperCreateRes createPaper(@RequestBody @Valid PaperCreateReq req) {
//		return new PaperCreateRes(
//				req.title(),
//				req.authors(),
//				req.year(),
//				req.url(),
//				System.currentTimeMillis()
//		);
//	}
//
//	@GetMapping("/api/hello")
//	public String hello() {
//		return "Hello, Spring Boot!";
//	}
//	
//	@GetMapping("/api/hello2")
//	public String hello2(@RequestParam(defaultValue="world") String name) {
//		return "Hello, "+ name + "!";
//	}
//	
//	@GetMapping("/api/hello/{name}")
//	public String helloPath(@PathVariable String name) {
//		return "Hello via path, "+ name + "!";
//	}
//	
//	record HelloRes(String message, long timestamp) {}
//	
//	@GetMapping("/api/hello/json")
//	public HelloRes helloJson(@RequestParam(defaultValue="world") String name) {
//		return new HelloRes("Hello, " + name + "!", System.currentTimeMillis());
//	}
//}
