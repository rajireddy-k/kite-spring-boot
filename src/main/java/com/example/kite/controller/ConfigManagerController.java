package com.example.kite.controller;


import com.example.kite.service.ConfigManagerService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;


import java.util.List;
import java.util.Map;
import java.util.Set;


@RestController
@RequestMapping("/api/config")
@AllArgsConstructor
public class ConfigManagerController {
    private final ConfigManagerService configService;


    @PostMapping("/setParams")
    public ResponseEntity<Map<String, Object>> setConfigProperty(@RequestBody Map<String, Object> configMap)  {
        configMap.entrySet().stream().forEach(
                entry ->   configService.setConfigMap(entry.getKey(), entry.getValue())
        );
        return new ResponseEntity<>(configMap,HttpStatus.CREATED);
    }


    @GetMapping("/view/configs")
    public ResponseEntity<Map<String, Object>> getConfigs()  {
        return new ResponseEntity<>(configService.getAllProperties(),HttpStatus.OK);
    }

    @PostMapping("/addWatchList")
    public ResponseEntity<Map<String, Set<String>>> addWatchList(@RequestBody Map<String, Set<String>> watchList)  {
        watchList.entrySet().stream().forEach(
                entry ->   configService.addWatchList(entry.getKey(), entry.getValue())
        );
        return new ResponseEntity<>(watchList,HttpStatus.CREATED);
    }

    @PostMapping("/addSymbol")
    public ResponseEntity<Map<String, String>> addSymbols(@RequestBody Map<String, String> watchList)  {
        watchList.entrySet().stream().forEach(
                entry ->   configService.addSymbol(entry.getKey(), entry.getValue())
        );
        return new ResponseEntity<>(watchList,HttpStatus.CREATED);
    }

    @DeleteMapping("/removeSymbol")
    public ResponseEntity<Map<String, String>> removeSymbol(@RequestBody Map<String, String> watchList)  {
        watchList.entrySet().stream().forEach(
                entry ->   configService.removeFromWatchList(entry.getKey(), entry.getValue())
        );
        return new ResponseEntity<>(watchList,HttpStatus.OK);
    }

    @PostMapping("/removeWatchList")
    public ResponseEntity<Map<String, List<String>>> removeWatchList(@RequestBody Map<String, List<String>> watchList)  {
        watchList.entrySet().stream().forEach(
                entry ->   configService.removeFromWatchList(entry.getKey(), entry.getValue())
        );
        return new ResponseEntity<>(watchList,HttpStatus.CREATED);
    }

    @GetMapping("/watchList/{action}")
    public ResponseEntity<Set<String>> removeWatchList(@PathVariable("action") String action)  {

        return new ResponseEntity<>(configService.getWatchlistForAction(action),HttpStatus.CREATED);
    }
}
